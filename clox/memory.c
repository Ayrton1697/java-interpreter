#include <stdlib.h>

#include "compiler.h"
#include "memory.h"
#include "vm.h"

#ifdef DEBUG_LOG_GC
#include <stdio.h>
#include "debug.h"
#endif

#define GC_HEAP_GROW_FACTOR 2
#define HEAP_SIZE (1024*1024)

void* from_space = NULL;
void* to_space = NULL;
void* alloc_ptr = NULL;

void* reallocate(void* pointer, size_t oldSize, size_t newSize){
    vm.bytesAllocated += newSize - oldSize;
    if(newSize > oldSize){
    #ifdef DEBUG_STRESS_GC
        collectGarbage();
    #endif
        if(vm.bytesAllocated > vm.nextGC){
            collectGarbage();
        }
    }
    if(newSize == 0){
        free(pointer);
        return NULL;
    }

    void* result = realloc(pointer,newSize);
    if(result == NULL) exit(1);
    return result;
}

void markObject(Obj* object){
    if(object == NULL) return;
    if(object->isMarked) return;
#ifdef DEBUG_LOG_GC
    printf("%p mark ", (void*)object);
    printValue(OBJ_VAL(object));
    printf("\n");
#endif

    object->isMarked = true;

    if(vm.grayCapacity < vm.grayCount + 1){
        vm.grayCapacity = GROW_CAPACITY(vm.grayCapacity);

        vm.grayStack = (Obj**)realloc(vm.grayStack, sizeof(Obj*) * vm.grayCapacity);

        if(vm.grayStack == NULL) exit(1);
    }

    vm.grayStack[vm.grayCount++] = object;
}

void markValue(Value value){
    if (IS_OBJ(value)) markObject(AS_OBJ(value));
}

static void markArray(ValueArray* array){
    for(int i = 0; i < array->count; i++){
        markValue(array->values[i]);
    }
}

void blackenObject(Obj* object){
#ifdef DEBUG_LOG_GC
    printf("%p blacken ", (void*)object);
    printValue(OBJ_VAL(object));
    printf("\n");
#endif
    switch (object->type)
    {
    case OBJ_CLOSURE:{
        ObjClosure* closure = (ObjClosure*)object;
        markObject((Obj*)closure->function);
        for(int i = 0; i < closure->upvalueCount; i++){
            markObject((Obj*)closure->upvalues[i]);
        }
        break;
    }
    case OBJ_FUNCTION: {
        ObjFunction* function = (ObjFunction*)object;
        markObject((Obj*)function->name);
        markArray(&function->chunk.constants);
        break;
    }
    case OBJ_UPVALUE:
        markValue(((ObjUpvalue*)object)->closed);
        break;
    case OBJ_NATIVE:
    case OBJ_STRING:
        break;
    }
}

static void freeObject(Obj* object){
    #ifdef DEBUG_LOG_GC
        printf("%p free type %d\n", (void*)object, object->type);
    #endif
    switch(object->type){
        case OBJ_NATIVE:
            FREE(ObjNative, object);
            break;
        case OBJ_STRING:{
            ObjString* string = (ObjString*)object;
            FREE_ARRAY(char, string->chars, string->length + 1);
            FREE(ObjString, object);
            break;
        }
        case OBJ_UPVALUE:{
            FREE(ObjUpvalue, object);
            break;
        }
        case OBJ_CLOSURE:{
            ObjClosure* closure = (ObjClosure*)object;
            FREE_ARRAY(ObjUpvalue*, closure->upvalues, closure->upvalueCount);
            FREE(ObjClosure, object);
            break;
        }
        case OBJ_FUNCTION:{
            ObjFunction* function = (ObjFunction*)object;
            freeChunk(&function->chunk);
            FREE(ObjFunction, object);
            break;
        }
    }
}

static void markRoots(){
    for(Value* slot = vm.stack; slot < vm.stackTop; slot++){
        markValue(*slot);
    }

    for(int i = 0; i < vm.frameCount; i++){
        markObject((Obj*)vm.frames[i].closure);
    }

    for(ObjUpvalue* upvalue = vm.openUpvalues;
    upvalue != NULL;
    upvalue = upvalue->next){
        markObject((Obj*)upvalue);
    }

    markTable(&vm.globals);
    markCompilerRoots();
}

static void traceReferences(){
    while(vm.grayCount > 0){
       Obj* object = vm.grayStack[--vm.grayCount];
       blackenObject(object);
    }
}

static void sweep(){
    Obj* previous = NULL;
    Obj* object = vm.objects;
    while(object != NULL){
        if(object->isMarked){
            object->isMarked = false;
            previous = object;
            object = object->next;
        } else {
            Obj* unreached = object;
            object = object->next;
            if(previous != NULL){
                previous->next = object;
            } else {
                vm.objects = object;
            }

            freeObject(unreached);
        }
    }
}

void collectGarbage(){
    #ifdef DEBUG_LOG_GC
        printf("--gc begin \n");
        size_t before = vm.bytesAllocated;
    #endif

    markRoots();
    traceReferences();
    tableRemoveWhite(&vm.strings);
    sweep();

    vm.nextGC = vm.bytesAllocated * GC_HEAP_GROW_FACTOR;

    #ifdef DEBUG_LOG_GC
        printf("--gc end \n");
        printf(" collected %zu bytes (from %zu to %zu) next at %zu\n",
        before - vm.bytesAllocated, before, vm.bytesAllocated,
        vm.nextGC);
    #endif
}

void freeObjects(){
    Obj* object = vm.objects;
    while(object != NULL){
        Obj* next = object->next;
        freeObject(object);
        object = next;
    }

    free(vm.grayStack);
}

void initHeaps(){
    from_space = malloc(HEAP_SIZE);
    to_space = malloc(HEAP_SIZE);

    if(from_space == NULL || to_space == NULL){
        fprintf(stderr,"Failed to allocate heaps!\n");
        exit(1);
    }
    alloc_ptr = from_space;
}

void* alloc_ptr;
extern void* to_alloc_ptr;

size_t get_object_size(Obj* obj){
    switch (obj->type)
    {
    case OBJ_STRING:
        return sizeof(OBJ_STRING) + ((ObjString*)obj)->length + 1; 
        break;
    case OBJ_FUNCTION:
        return sizeof(ObjFunction); 
        break;
    case OBJ_CLOSURE:
        ObjClosure* closure = (ObjClosure*)obj;
        return sizeof(ObjClosure) + (sizeof(ObjUpvalue*) * closure->upvalueCount); 
        break;
    case OBJ_UPVALUE:
        return sizeof(ObjUpvalue); 
        break;
    case OBJ_NATIVE:
        return sizeof(ObjNative); 
        break;
    case OBJ_FORWARDED:
        return;
        break;
    default:
        fprintf(stderr, "Error: Unknown object type in GC!\n");
        exit(1);
    }

}

Value copy_and_forward(Obj* obj){

    // copy its data to that pointer
    size_t size = get_object_size(obj);
    void* og_address = obj;
    to_alloc_ptr = (char*)to_alloc_ptr + size;
    void* new_address = memcpy(to_alloc_ptr, obj, size);

    ForwardingPtr* tombstone = (ForwardingPtr*)og_address;
    tombstone->new_address = new_address;
    tombstone->type = OBJ_FORWARDED;
    // return updated pointer
    return OBJ_VAL(new_address);
}

static void scanObject(Obj* obj){
   switch (obj->type)
    {
    case OBJ_STRING: 
        break;
    case OBJ_FUNCTION:
        ObjFunction* function = (ObjFunction*)obj;
        
        // move name from FROM HEAP to TO HEAP
        Value new_name_address = copyValue(OBJ_VAL(function->name)); 
        function->name = AS_OBJ(new_name_address); 
        
        // Chunk chunk = (Chunk)function->chunk->constants;
        ValueArray constants = function->chunk.constants;
        for(int i = 0; i < constants.count; i++){
            Value value = copyValue((Value)constants.values[i]); 
            constants.values[i] = value;
        }

        break;
    case OBJ_CLOSURE:   
        ObjClosure* closure = (ObjClosure*)obj; 
        break;
    case OBJ_UPVALUE: 
        break;
    case OBJ_NATIVE:
        break;
    case OBJ_FORWARDED:;
        break;
    default:
        fprintf(stderr, "Error: Unknown object type in GC!\n");
        exit(1);
    }
}

// copies value from FROM HEAP to TO HEAP
Value copyValue(Value val){
    if (!IS_OBJ(val)) { // Assuming you have a macro like this
        return val;
    }
    // check if its not already on to_Heap
    Obj* obj = AS_OBJ(val);
    void* heap_end = (char*)from_space + HEAP_SIZE;
    if((void*)obj >= from_space && (void*)obj < heap_end ){
        if(obj->type == OBJ_FORWARDED){
            ForwardingPtr* tombstone = (ForwardingPtr*)obj;
            return OBJ_VAL(tombstone->new_address);
        } else {
            copy_and_forward(obj);
            scanObject(obj);
        }
    } else {
        void* to_ptr = memcpy(to_ptr,value->startPointer, sizeof(val));
        return OBJ_VAL(void*);
    }

}


static void markRootsnewGC(){
    // stack roots
    for(Value* slot = vm.stack; slot < vm.stackTop; slot++){
        // markValue(*slot);
        *slot = copyValue(*slot);
    }
    // vm frames roots
    for(int i = 0; i <= vm.frameCount; i++){
        ObjClosure* closure = vm.frames[i].closure;
        markObject((Obj*)closure);
    }
    // los upvalues
    for(ObjUpvalue* upvalue = vm.openUpvalues;
        upvalue != NULL;
        upvalue = upvalue->next){
            markObject((Obj*)upvalue);
    }
    // vm global variable roots
    markTable(&vm.globals);
    markCompilerRoots();
}

void* triggerCollection(){
    // pause vm execution

    // go through list of roots and mark
    markRootsnewGC();
    
    // move all reachable items to new heap
    
    // remove all items from old heap

}

void* gcAllocator(size_t size) {

    void* new_alloc_ptr = (char*)alloc_ptr + size;
    void* end_of_heap =  (char*)from_space + HEAP_SIZE;

    if(new_alloc_ptr >= end_of_heap){
        triggerCollection();
    }
    void* user_ptr = alloc_ptr;
    alloc_ptr = (char*)alloc_ptr + size;
    return user_ptr;
}