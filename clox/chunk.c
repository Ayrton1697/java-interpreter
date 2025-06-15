#include <stdlib.h>

#include "chunk.h"
#include "memory.h"

void initChunk(Chunk* chunk){
    chunk->count = 0;
    chunk->capacity = 0;
    chunk->code = NULL;
    chunk->lines = NULL;
    initValueArray(&chunk->constants);
}

void freeChunk(Chunk* chunk){
    FREE_ARRAY(uint8_t, chunk->code, chunk->capacity);
    FREE_ARRAY(int, chunk->lines, chunk->capacity);
    freeValueArray(&chunk->constants);
    initChunk(chunk);
}

void writeChunk(Chunk* chunk, uint8_t byte, int line){
    if(chunk->capacity < chunk->count +1){
        int oldCapacity = chunk->capacity;
        chunk->capacity = GROW_CAPACITY(oldCapacity);
        chunk->code = GROW_ARRAY(uint8_t, chunk->code,oldCapacity,chunk->capacity);
        // chunk->lines = GROW_ARRAY(int, chunk->lines,oldCapacity,chunk->capacity);
    }

    if(line != chunk->lines[chunk->lineCount - 1].line){
        if(chunk->lineCapacity < chunk->lineCount + 1){
            int oldLineCapacity = chunk->lineCapacity;
            chunk->lineCapacity = GROW_CAPACITY(oldLineCapacity);
            chunk->lines = GROW_ARRAY(LineRun, chunk->lines, oldLineCapacity, chunk->lineCapacity);
        }
        LineRun* run = &chunk->lines[chunk->lineCount];
        run->line = line;
        run->count = 1;
        chunk->lineCount++;
    } else {
        chunk->lines[chunk->lineCount - 1].count++;
    }

    chunk->code[chunk->count] = byte;
    // chunk->lines[chunk->count] = line;
    chunk->count++;
}

int addConstant(Chunk* chunk, Value value){
    writeValueArray(&chunk->constants, value);
    return chunk->constants.count - 1;
}

// move this to error reporting file
int getLine(Chunk* chunk, int offset){
    int currentOffset = 0;
    for(int i = 0; i < chunk->lineCount; i++){
        LineRun* run = &chunk->lines[i];
        if(offset < currentOffset +  run->count){
            return run->line;
        }
        currentOffset+= run->count;
    }
    return -1;
}
