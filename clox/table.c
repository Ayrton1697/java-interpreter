#include <stdlib.h>
#include <string.h>

#include "memory.h"
#include "object.h"
#include "table.h"
#include "value.h"

#define TABLE_MAX_LOAD 0.75

void initTable(Table* table){
    table->count = 0;
    table->capacity = 0;
    table->entries =  NULL;
}

void freeTable(Table* table){
    FREE_ARRAY(Entry, table->entries, table->capacity);
    initTable(table);
}

static Entry* findEntry(Entry* entries, int capacity, Value key, int hash){
    uint32_t index = hash % capacity;
    Entry* tombstone = NULL;
    for(;;){
        Entry* entry = &entries[index];
        if(IS_NIL(entry->key)){
            if(IS_NIL(entry->value)){
                // empty entry
                return tombstone != NULL ? tombstone : entry;
            } else {
                if(tombstone == NULL) tombstone = entry;
            } 
        }  else if(valuesEqual(entry->key, key)) {
            return entry;
        }

        index = (index + 1) % capacity;
    }
}

bool tableGet(Table* table, Value key, Value* value){
    if(table->count == 0) return false;
    uint32_t  hash = hashValue(key);
    Entry* entry = findEntry(table->entries, table->capacity, key, hash);
    if(IS_NIL(entry->key)) return false;

    *value = entry->value;
    return true;
}

static void adjustCapacity(Table* table, int capacity){
    Entry* entries = ALLOCATE(Entry, capacity);
    for(int i = 0; i < capacity; i++){
        entries[i].key = NIL_VAL;
        entries[i].value = NIL_VAL;
    }
    table->count = 0;
    for(int i = 0; i < table->capacity; i++){
        Entry* entry = &table->entries[i];
        if(IS_NIL(entry->key) == NULL) continue;
        Entry* dest = findEntry(entries, capacity, entry->key, entry->hash);
        dest->key = entry->key;
        dest->value = entry->value;
        table->count++;
    }

    FREE_ARRAY(Entry, table->entries, table->capacity);
    table->entries = entries;
    table->capacity = capacity;
}   

int hashValue(Value key){
    switch (key.type)
    {
    case VAL_BOOL:return 0;
    case VAL_NUMBER: return 1;
    case VAL_NIL: return 0;
    case VAL_OBJ: return 0;
    break;
    }
}

bool tableSet(Table* table, Value key, Value value){
    if(table->count +1 > table->capacity * TABLE_MAX_LOAD){
        int capacity = GROW_CAPACITY(table->capacity);
        adjustCapacity(table, capacity);
    }
    uint32_t  hash = hashValue(key);
    Entry* entry = findEntry(table->entries, table->capacity, key, hash);
    bool isNewKey = IS_NIL(entry->key);
    if(isNewKey && IS_NIL(entry->value)) table->count++;

    entry->key = key;
    entry->value = value;
    return isNewKey;
}

bool tableDelete(Table* table, Value key){
    if(table->count == 0) return false;
    uint32_t  hash = hashValue(key);
    Entry* entry = findEntry(table->entries, table->capacity, key, hash);
    if(entry == NULL) return false;
    entry->key = NIL_VAL; //este tipo no existe, habria que agregarlo
    entry->value = BOOL_VAL(true);
    return true;
}

void tableAddAll(Table* from, Table* to){
    for(int i = 0; i < from->capacity; i++){
            Entry* entry = &from->entries[i];
            if(!IS_NIL(entry->key)){
                tableSet(to, entry->key, entry->value);
            }
    }
}

ObjString* tableFindString(Table* table, const char* chars, int length, uint32_t hash){
    if(table->count == 0) return NULL;
    uint32_t index = hash % table->capacity;
    for(;;){
        Entry* entry = &table->entries[index];
        if(IS_NIL(entry->key) == NULL){
            if(IS_NIL(entry->value)) return NULL;
        } else if (
            entry->key->length == length && 
            entry->hash == hash &&
            memcmp(entry->key->chars, chars, length) == 0){
            
            return entry->key;
        }
        index = (index + 1) % table->capacity;
    }
}