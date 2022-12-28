# Prototype

Prototype is a library for defining and loading prototypes from json files.

## Add to your dependencies

Gradle:

    implementation "com.github.cleverelephant.prototype:prototype:LATEST"
    
Maven:

    <dependency>
      <groupId>com.github.cleverelephant.prototype</groupId>
      <artifactId>prototype</artifactId>
      <version>LATEST</version>
    </dependency>
    
## Usage

1. Define your prototype
```java
public interface MyPrototype implements Prototype<MyData>
{
    String foo();
    int[] bar();
    Set<Double> baz();
}
```
2. Code your prototype builder
```java
public class MyPrototypeBuilder implements PrototypeBuilder<MyData, MyPrototype>
{
    @Override
    public MyData build(MyPrototype proto, String arg)
    {
        doSomeOperation(proto);
        return new MyData(...);
    }
}
```
3. Register your builder
```java
@DefaultProtoBuilder(MyPrototypeBuilder.class)
public interface MyPrototype ...
```
   or
```java
PrototypeManager.registerBuilder(MyPrototype.class, new MyPrototypeBuilder());
```
4. Define your prototype data in json format
```json
{
  "$prototypeClass": "com.mypackage.MyPrototype",
  "foo": "abc",
  "bar": 42,
  "baz": [ 0.1, 0.2, 3.14 ]
}
```
5. Load your prototypes
```java
PrototypeManager.loadPrototypes(Path.of("path/to/prototypeRoot"), null);
```
6. Use your prototype
```java
Optional<MyPrototype> proto = PrototypeManager.getPrototype("path/to/my/prototype");
MyData data = PrototypeManager.createType("path/to/my/prototype");
```
