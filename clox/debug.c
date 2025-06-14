#include <stdio.h>

#include "debug.h"

void dissasembleChunk(Chunk* chunk, const char* name){
    print("== %s == \n", name);
    for(int offset=0; offset < chunk->count;){
        offset = dissasembleInstruction(chunk, offset);
    }
}

int dissasembleInstruction(Chunk* chunk, int offset){
    print("%04d ", offset);

    uint8_t instruction = chunk->code[offset];
    switch (instruction)
    {
    case OP_RETURN:
        return simpleInstruction("OP_RETURN", offset);
    default:
        print("Unknown opcode %d\n", instruction);
        return offset +1;
    }
}