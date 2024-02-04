[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![GPL License][license-shield]][license-url]



<!-- PROJECT LOGO -->
<br />
<div align="center">
<h3 align="center">Prototype</h3>

  <p align="center">
    A small library for loading prototypes from lua files in java.
    <!--<br />
    <a href="https://github.com/benjaminwied/Prototype/wiki"><strong>Explore the docs »</strong></a>-->
    <br />
    <br />
    <a href="https://github.com/benjaminwied/Prototype">View Code</a>
    ·
    <a href="https://github.com/benjaminwied/Prototype/issues">Report Bug</a>
    ·
    <a href="https://github.com/benjaminwied/Prototype/discussions">Request Feature</a>
  </p>
</div>

## Built With

* [luaj](https://github.com/luaj/luaj)
* [Jackson](https://github.com/FasterXML/jackson)

## Usage

Prototypes can be used to build various objects from properties.
Fist, create your desired type just as a normal java class:
```java
public class MyClass {
  ...
  public MyClass(String a, boolean b, long createTime) {
    ...
  }
}
```

Then write your prototype class (this will be deserialized usin Jackson):
```java
public class MyPrototype extends Prototype<MyClass> {
    public String a;
    public boolean b;
    
    @Override
    public MyClass build() {
        return new MyClass(a, b, System.currentTimeMillis());
    }
}
```

Define the values in lua:
```lua
prototypes["examplePrototype"] = {
    class = "com.example.MyPrototype",
    data = {
        a = "abc",
        b = true
    }
}
```

To load and use prototypes, do the following:
```java
Path rootPath = Path.of("path/to/your/prototypes");
Map<String, Object> context = Map.of("exampleKey", "42");
PrototypeManager.loadPrototypes(rootPath, context);

// optional: verify integrity
IntegrityChecker.verifyIntegrity();

// get a prototype
Prototype<MyClass, ? extends Prototype<MyClass>> prototype = PrototypeManager.getPrototype("examplePrototype");

// build the type
MyClass myObject = prototype.build();

// or, if you only need the type
myObject = PrototypeManager.createType("examplePrototype");
```

Prototypes can use the context to inject additional data.
```lua
local valA = exampleKey;

prototypes["anotherPrototype"] = {
    class = "com.example.MyPrototype",
    data = {
        a = valA,
        b = false
    }
}

-- Using the above loading code, "a" would have be "42"
```

<!--_For more examples, please refer to the [Documentation](https://example.com)_-->

## Installation

Use gradle
```gradle
implementation 'io.github.benjaminwied:prototype:0.3.0'
```

## Contributing

Contributions are what make the open source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

If you have a suggestion that would make this better, please fork the repo and create a pull request. You can also simply open an issue with the tag "enhancement".
Don't forget to give the project a star! Thanks again!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

Distributed under the MIT License. See `LICENSE.txt` for more information.


## Contact

Project Link: [https://github.com/benjaminwied/Prototype](https://github.com/benjaminwied/Prototype)


## Acknowledgments

* [Othneil Drew](https://github.com/othneildrew/Best-README-Template) for his README-Template.

<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->
[contributors-shield]: https://img.shields.io/github/contributors/benjaminwied/Prototype.svg?style=flat
[contributors-url]: https://github.com/benjaminwied/Prototype/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/benjaminwied/Prototype.svg?style=flat
[forks-url]: https://github.com/benjaminwied/Prototype/network/members
[stars-shield]: https://img.shields.io/github/stars/benjaminwied/Prototype.svg?style=flat
[stars-url]: https://github.com/benjaminwied/Prototype/stargazers
[issues-shield]: https://img.shields.io/github/issues/benjaminwied/Prototype.svg?style=flat
[issues-url]: https://github.com/benjaminwied/Prototype/issues
[license-shield]: https://img.shields.io/github/license/benjaminwied/Prototype.svg?style=flat
[license-url]: https://github.com/benjaminwied/Prototype/blob/master/LICENSE
