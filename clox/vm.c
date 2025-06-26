#include "stdio.h"
#include "common.h"
#include "debug.h"
#include "vm.h"

VM vm;

static void resetStack(){
    vm.capacity = 8;
    if (vm.stack != NULL){
        free(vm.stack);
    }
    vm.stack = (Value*)malloc(vm.capacity * sizeof(Value));
    if(vm.stack == NULL) exit(1);
    vm.stackTop = vm.stack;
    vm.size = 0;
}

void* reallocate(void* ptr,size_t new_size){
    if(new_size == 0){
        free(ptr);
        return NULL;
    }
        void* new_ptr = realloc(ptr,new_size * sizeof(Value));
        if (new_ptr == NULL) exit(1);
        return new_ptr;
}

void initVM(){
    resetStack();
}

void freeVM(){

}

void push(Value value){
    if(vm.size >= vm.capacity){
        size_t oldCapacity = vm.capacity;
        int newCap = oldCapacity * 2;
        vm.capacity = newCap;
        size_t stack_offset = vm.stackTop - vm.stack;
        vm.stack = reallocate(vm.stack,newCap);
        vm.stackTop = vm.stack + stack_offset;
    }
    *vm.stackTop = value;
    vm.stackTop++;
    vm.size++;
}

Value pop(){
    vm.stackTop--;
    vm.size--;
    return *vm.stackTop;
}

static InterpretResult run(){
    #define READ_BYTE() (*vm.ip++)
    #define READ_CONSTANT() (vm.chunk->constants.values[READ_BYTE()])
    #define BINARY_OP(op) \
        do { \
            double b = pop(); \
            double a = pop(); \
            push(a op b); \
        } while (false)

    for (;;){

        #ifndef DEBUG_TRACE_EXECUTION
            printf("    ");
            for (Value* slot = vm.stack; slot < vm.stackTop; slot++ ){
                printf("[ ");
                printValue(*slot);
                printf(" ]");
            }
            print("\n");
            dissasembleInstruction(vm.chunk, (int)(vm.ip - vm.chunk->code));
        #endif
        uint8_t instruction;
        switch (instruction = READ_BYTE()){
            case OP_CONSTANT:{
                Value constant = READ_CONSTANT();
                push(constant);
                break;
            }
            case OP_ADD: BINARY_OP(+); break;
            case OP_SUBSTRACT: BINARY_OP(-); break;
            case OP_MULTIPLY: BINARY_OP(*); break;
            case OP_DIVIDE: BINARY_OP(/); break;
            // case OP_NEGATE: push(-pop()); break;
            case OP_NEGATE: 
                if (vm.stackTop == vm.stack) { //si esta vacio no podemos buscar el index -1
                    fprintf(stderr, "Stack underflow in OP_NEGATE.\n");
                    exit(1);
                } 
                vm.stackTop[-1] = (Value) - vm.stackTop[-1];
            case OP_RETURN:{
                printValue(pop());
                printf("\n");
                return INTERPRET_OK;
            }
        
        }
    }
    #undef READ_BYTE
    #undef READ_CONSTANT
    #undef BINARY_OP
}

InterpretResult interpret(Chunk* chunk){
    vm.chunk = chunk;
    vm.ip = vm.chunk->code;
    return run();
}