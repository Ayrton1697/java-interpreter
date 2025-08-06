#include <stdio.h>
#include <string.h>

#include "memory.h"
#include "object.h"
#include "value.h"
#include "vm.h"

#define ALLOCATE_OBJ(type, objectType) \
    (type*)allocateObject(sizeof(type), objectType)


static Obj* allocateObject(size_t size, ObjType type){
    Obj* object = (Obj*)reallocate(NULL,0,size);
    object->type = type;

    object->next = vm.objects;
    vm.objects = object;
    return object;
}

static ObjString* allocateString(size_t size, int length){
    ObjString* string = (ObjString*)allocateObject(size, OBJ_STRING);
    string-> length = length;
    return string;
}

ObjString* takeString(char* chars, int length){
    return allocateString(chars, length);
}

ObjString* copyString(const char* chars, int length){
    ObjString* string = ALLOCATE_OBJ(ObjString, OBJ_STRING);
    char* heapChars = ALLOCATE(char, length + 1);
    memcpy(heapChars, chars, length);
    heapChars[length] = '\0';
    return allocateString(heapChars, length);
}

ObjString* copyStringEfficient(const char* chars, int length){
    size_t size = sizeof(ObjString) + length + 1;
    ObjString* string = allocateString(size, length);
    memcpy(string->chars, chars, length);  
    string->chars[length] = '\0' ;
    return string;
}

void printObject(Value value){
    switch (OBJ_TYPE(value))
    {
    case OBJ_STRING:
        printf("%s", AS_CSTRING(value));
        break;
    }
}