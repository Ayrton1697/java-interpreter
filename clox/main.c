#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "common.h"
#include "chunk.h"
#include "debug.h"
#include "vm.h"

static void repl(){
 char line[1024];
 for (;;){
    printf("> ");
    if(!fgets(line, sizeof(line), stdin)){
        printf("\n");
        break;
    }
 }
}

int main(int argc, const char* argv[]){
    
    initVM();
    
    if(argc == 1){
        repl();
    } else if (argc == 2){
        runfile(argv[1]);
    } else {
        fprintf(stderr, "Usage: clox [path]'n");
        exit(64);
    }
    freeVM();
    return 0;
}