grammar Prototype;

prototype: action EOF;

action: addAction;
addAction: ADD keyValue;

keyValue: key ':' value;
key: STRINGLITERAL;
value: object | array | STRINGLITERAL | NUMBER | BOOLEAN;
object: '{' '}' | '{' keyValue (',' keyValue)* '}';
array: '[' ']' | '[' value (',' value)* ']';

ADD: 'add';
NUMBER
   : INT ('.' [0-9]*)? EXP? // +1.e2, 1234, 1234.5
   | '.' [0-9]+ EXP?        // -.2e3
   | '0' [xX] HEX+          // 0x12345678
   ;
   
BOOLEAN: 'true' | 'false';
STRINGLITERAL: '"' ~'"'* '"';

WS: [ \t\r\n] -> skip;

fragment SIGN: '+' | '-';
fragment HEX: [0-9a-fA-F];
fragment INT: '0' | [1-9] [0-9]*;
fragment EXP: [Ee] SIGN? [0-9]*;