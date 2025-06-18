#ifndef clox_chunk_h
#define clox_chunk_h

#include "common.h"
#include "value.h"

typedef enum {
    OP_CONSTANT,
    OP_RETURN,
    OP_CONSTANT_LONG,
} OpCode;

typedef struct{
    int line;
    int count;
} LineRun;

typedef struct {
    int count;
    int capacity;
    uint8_t* code;
    // int* lines;
    ValueArray constants;
    LineRun* lines;
    int lineCount; //tamaño del array de LineRun
    int lineCapacity; //tamaño alocado del array de lines
} Chunk;

void initChunk(Chunk* chunk);
void freeChunk(Chunk* chunk);
void writeChunk(Chunk* chunk, uint8_t byte, int line);
int addConstant(Chunk* chunk, Value value);
int writeConstant(Chunk* chunk, Value value, int line);

#endif