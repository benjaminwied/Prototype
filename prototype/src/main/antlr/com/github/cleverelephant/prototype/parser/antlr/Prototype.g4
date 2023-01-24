grammar Prototype;

prototype: header content EOF;

header: classDefinition?;
content: action*;

/* Header */
classDefinition: 'class' STRINGLITERAL;

/* Actions */
action: setAction | addAction | replaceAction | removeAction | renameAction | modifyAction | applyFromAction | conditionalAction;
setAction: 'set' keyValue;
addAction: 'add' keyValue;
replaceAction: 'replace' keyValue;
removeAction: 'remove' key;
renameAction: 'rename' key 'into' key;
modifyAction: 'modify' key ':' '{' action (',' action)* '}';
applyFromAction: 'inherit' 'from' STRINGLITERAL;
conditionalAction: 'if' '(' STRINGLITERAL ')' ':' '{' action (',' action)* '}';

/* JSON-Content */
keyValue: key ':' value;
key: STRINGLITERAL;
value: object | array | STRINGLITERAL | NUMBER | BOOLEAN;
object: '{' '}' | '{' keyValue (',' keyValue)* '}';
array: '[' ']' | '[' value (',' value)* ']';

/* Lexer rules */
NUMBER
   : INT ('.' [0-9]*)? EXP? // +1.e2, 1234, 1234.5
   | '.' [0-9]+ EXP?        // -.2e3
   | '0' [xX] HEX+          // 0x12345678
   ;
   
BOOLEAN: 'true' | 'false';
STRINGLITERAL: '"' ~'"'* '"';
OPERATOR: '==' | '>' | '<' | '>=' | '<=' | '!=';

WS: [ \t\r\n] -> skip;

fragment SIGN: '+' | '-';
fragment HEX: [0-9a-fA-F];
fragment INT: '0' | [1-9] [0-9]*;
fragment EXP: [Ee] SIGN? [0-9]*;
fragment LETTER: [a-zA-Z];
