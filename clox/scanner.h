#ifndef clox_scanner_h
#define clox_scanner_h

typedef enum {
    //  Single character tokens
    TOKEN_LEFT_PAREN, TOKEN_RIGHT_PAREN,
} TokenType;

typedef struct {
    int line;
    int length;
    const char* start;
    TokenType type;
} Token;

void initScanner(const char* source);
Token scanToken();
#endif