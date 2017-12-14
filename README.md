# BuilderProcessor

> 我们平常在使用Java进行开发时，经常会需要写很多重复冗余的样板代码(Boilerplate Code)，Android开发中最常见的一种，就是***findViewById***了，如果一个界面有很多**View**，写起来那叫一个要死要死。
于是神一样的Jake Wharton开源了一个`ButterKnife`，让我们从此跟***findViewById***撒哟啦啦。而`ButterKnife`的核心原理就是使用了注解处理器在编译时期帮我们生成了这些样板代码。
今天我们来初探一下，如何通过打造一个注解处理器来消除样板代码。

## 1. 从一个例子开始

我们先来看这样一个类：

```
public class User {
    String firstName;
    String lastName;
    String nickName;
    int age;

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getNickName() {
        return nickName;
    }

    public int getAge() {
        return age;
    }
}
```

一个超简单的类，就四个属性跟它对应的`Getter`。

这个类目前没有构造函数，如果我们给它添加构造函数的话，可能要根据不同属性重载好多个，很麻烦，这样我们不如给他写个Builder类：

```
public final class UserBuilder {
  private String firstName;
  private String lastName;
  private String nickName;
  private int age;

  public UserBuilder firstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  public UserBuilder lastName(String lastName) {
    this.lastName = lastName;
    return this;
  }

  public UserBuilder nickName(String nickName) {
    this.nickName = nickName;
    return this;
  }

  public UserBuilder age(int age) {
    this.age = age;
    return this;
  }

  public User build() {
    User user = new User();
    user.firstName = this.firstName;
    user.lastName = this.lastName;
    user.nickName = this.nickName;
    user.age = this.age;
    return user;
  }
}
```
可以看到，**UserBuilder**类中包含**User**类中的全部属性，然后是属性对应的Setter，最后还有一个build方法，用来创建**User**实例。

这时候假设你有另外一个类，你也想给它写一个Builder类，你会发现，Builder类的写法是固定的，`属性`加`Setter`加`build`方法，这很显然就是样板代码了，既然这样，那我们能不能通过某种方式自动给一个类生成他对应的Builder类呢？

当然可以啦，通过注解处理和代码生成就可以轻松实现。我们接下来分别详细看看这两部分。

## 2. 注解处理(Annotation Processing)

注解处理是`javac`的一部分，可以在编译时期扫描注解并进行处理。
注解处理从Java 5就出现了，但直到Java 6才有了可用的API。

### 2.1 注解(Annotation)
既然是写注解处理器，我们肯定需要先定义一个注解，然后再处理吧。

因此我们先声明一个叫**Builder**的注解：

```
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Builder {

}
```
声明一个注解跟声明一个接口很像，只不过注解的interface关键字前有个@符号。这个注解又使用了两个Java提供的元注解。

第一个元注解**@Target**，用来指明当前注解类型的使用对象。
这里用的是**ElementType.TYPE**，表示该注解可以用于类，接口，或是枚举。
除此之外Java还提供了：

- **ElementType.FIELD** 指明注解可应用于属性
- **ElementType.METHOD** 指明注解可应用于方法
- **ElementType.PARAMETER** 指明注解可应用于参数
- **ElementType.CONSTRUCTOR** 指明注解可应用于构造函数
- 还有很多，寄几去看

第二个元注解**@Retention**，用来指明当前注解类型的保留机制，Java提供了三种注解保留机制：

- **RetentionPolicy.SOURCE** 注解在编译完后被抛弃，不会出现在`.class`文件中。
- **RetentionPolicy.CLASS** 注解保留在编译后的`.class`文件中，但不会被加载到JVM中，这是Java默认的保留机制。
- **RetentionPolicy.RUNTIME** 注解保留在编译后的`.class`文件中，会被加载到JVM中，运行的时候可以通过反射获取到。

这里我们使用的是RetentionPolicy.SOURCE，因为源码中的注解处理完后，我们就不再需要了。

### 2.2 注解处理器(Annotation Processor)

实现一个自己的注解处理器，需要创建一个类并继承**AbstrctProcessor**。需要注意的是，该类**必须包含**一个**无参构造函数**。

```
public class BuilderProcessor extends AbstractProcessor {

}
```
我们这里的注解处理器用来处理前面写好的**@Builder**注解，因此就叫**BuilderProcessor**。继承**AbstrctProcessor**类后，有下面这四个方法需要重写：

```
public class BuilderProcessor extends AbstractProcessor {
    private Messager messager;
    private Elements elementUtils;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(Builder.class.getCanonicalName());
    }
    
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Builder.class)) {
            // ...
        }
        return true;
    }
}
```
1. ***init(ProcessingEnvironment processingEnv)***方法
该方法用来初始化处理器，同时该方法传入一个**ProcessingEnvironment**对象，我们可以从该对象获取到一些工具类的实例，我们这里获取到了messager，elementUtils和filer。
messager对象可以用来在注解处理过程中报错，提出警告。
elementUtils对象可以用来操作当前处理的元素。
filer对象用来写`.java`文件.

2. ***getSupportedSourceVersion()***方法
返回当前注解处理器支持的Java源码版本，这有点类似Android中的`targetSdkVersion`，所以一般返回最新的版本就可以，这里返回了**SourceVersion**.*latestSupported()*。

3. ***getSupportedAnnotationTypes()***方法
返回当前注解处理器支持处理的全部注解。
该方法的返回类型是一个**String**类型的**Set**集合，**Set**集合中每个元素应该是一个注解的完整全名(包名跟类名)。
由于我们这个处理器只处理**@Builder**注解，因此返回了**Collections**.*singleton*(**Builder**.class.*getCanonicalName()*)。
*singleton*是**Collections**类中的一个静态方法，会返回一个**SingletonSet**对象。
**Builder**.class.*getCanonicalName()*是获取**@Builder**注解带包名的完整全名。

4. ***process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)***方法
最重要的就是*process*方法了，因为这个方法实现了真正的注解处理生成代码的逻辑。
该方法在处理的过程中可能会被调用好几次。
该方法包含两参数，annotations和roundEnv，annotations是需要被处理的注解集合，roundEnv是Java提供的一个实现了**RoundEnvironment**接口的类的对象，该对象最常用的方法就是***getElementsAnnotatedWith(Class<? extends Annotation> a)***。


### 2.3 注解处理器配置文件

注解处理器需要注册后才能使用，注册的方法是在注解处理器模块的`main`文件夹下，创建一个叫`resources`的新文件夹，然后在该文件夹下创建一个`META-INF`文件夹，接着在`META-INF`文件夹下创建一个`services`文件夹，然后在`services`文件夹下创建一个叫`javax.annotation.processing.Processor`的文件，最后往这个文件里添加你注解处理器类的全名。麻烦爆了啊有没有！！！

好消息是，我们可以使用Google开源的一个`AutoService`的库，它会帮助我们自动生成这个配置文件，使用起来也很简单，先添加依赖：

```
dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    implementation project(':annotation')
    implementation 'com.google.auto.service:auto-service:1.0-rc3'
}
```
然后给我们的BuilderProcessor类加上AutoService注解就可以了：
```
@AutoService(Processor.class)
public class BuilderProcessor extends AbstractProcessor {
    //...
}
```

## 3. 代码生成(Code Generation)

代码生成即生成包含可运行Java源码的`.java`文件。

我们可以直接拼接字符串来生成代码，但这样很麻烦，而且生成的源码出错几率很大，因此这里使用一个叫`JavaPoet`的库来生成源码。

### 3.1 JavaPoet

`JavaPoet`是Square开源的一个用来生成`.java`源码的库，它提供了非常流畅的API，写起来也非常优雅，我们来简单看一下它怎么使用。

现在假设我们想生成一个超简单的**HelloWorld**类：
```
package com.example.helloworld;

public final class HelloWorld {
  public static void main(String[] args) {
    System.out.println("Hello, JavaPoet!");
  }
}
```
使用`JavaPoet`对应的生成代码如下：
```
MethodSpec main = MethodSpec.methodBuilder("main")
    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
    .returns(void.class)
    .addParameter(String[].class, "args")
    .addStatement("$T.out.println($S)", System.class, "Hello, JavaPoet!")
    .build();

TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
    .addMethod(main)
    .build();

JavaFile javaFile = JavaFile.builder("com.example.helloworld", helloWorld)
    .build();

javaFile.writeTo(System.out);
```

**MethodSpec**对应Java中的方法或是构造函数，**TypeSpec**对应类，接口或是枚举。除此之外`JavaPoet`还提供了**FieldSpec**来对应属性，**AnnotationSpec**对应注解，**ParameterSpec**对应参数。

**JavaFile**对应一个包含顶层类的Java源文件。

### 3.2 process方法的具体实现

初步了解了`JavaPoet`后，我们可以尝试着来具体实现我们的*process*方法.

首先我们获取所有含**@Builder**注解的元素：
```
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Builder.class)) {
            //...
        }
        return true;
    }
```
roundEnv.*getElementsAnnotatedWith*(Builder.class)的返回类型是`Set<? extends Element>`，即返回所有含**@Builder**注解的元素的集合。**Element**可以用来表示一个类，一个方法，或是一个变量，等等。

所以接下来我们判断这个元素是不是一个类，不是的话我们就报错：
```
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Builder.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                onError("Builder annotation can only be applied to class", element);
                return false;
            }
            //...
        }
        return true;
    }
```
然后我们开始获取一下接下来要用的参数，包括包名，当前元素的类名：
```
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Builder.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                onError("Builder annotation can only be applied to class", element);
                return false;
            }

            String packageName = elementUtils.getPackageOf(element).getQualifiedName().toString();
            String elementName = element.getSimpleName().toString();
            ClassName builderClassName = ClassName.get(packageName, String.format("%sBuilder", elementName));
            
            //...
        }
        return true;
    }
```
这里包名就是element表示的类所在的包名，elementName即element元素的类名，然后根据该类名，我们可以创建Builder的类名，比如说当前的类名是User，它的Builder的类名就是UserBuilder。

接下来我们使用`JavaPoet`逐步写出生成代码。

首先一个Builder包含属性和setter，因此我们先从element对象获取它的全部属性元素：

```
    private Set<Element> getFields(Element element) {
        Set<Element> fields = new LinkedHashSet<>();
        for (Element enclosedElement : element.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.FIELD) {
                fields.add(enclosedElement);
            }
        }
        return fields;
    }
    
```
***getFields***方法从element中获取全部类型为ElementKind.FIELD的元素并返回。
然后我们为这些元素生成`JavaPoet`对应的FieldSpec和MethodSpec：
```
private TypeSpec createTypeSpec(Element element, ClassName builderClassName, String elementName) {
        Set<Element> fieldElements = getFields(element);
        List<FieldSpec> fieldSpecs = new ArrayList<>(fieldElements.size());
        List<MethodSpec> setterSpecs = new ArrayList<>(fieldElements.size());
        for (Element field : fieldElements) {
            TypeName fieldType = TypeName.get(field.asType());
            String fieldName = field.getSimpleName().toString();
            FieldSpec fieldSpec = FieldSpec.builder(fieldType, fieldName, Modifier.PRIVATE).build();
            fieldSpecs.add(fieldSpec);
            MethodSpec setterSpec = MethodSpec
                    .methodBuilder(fieldName)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(builderClassName)
                    .addParameter(fieldType, fieldName)
                    .addStatement("this.$N = $N", fieldName, fieldName)
                    .addStatement("return this")
                    .build();
            setterSpecs.add(setterSpec);
        }
    //...
}
```

属性对应的的**FieldSpec**和Setter对应**MethodSpec**生成后，Builer类还差一个build方法，接下来我们就来生成这个代表build方法的**MethodSpec**对象：

```
    private TypeSpec createTypeSpec(Element element, ClassName builderClassName, String elementName) {
        //...
        TypeName elementType = TypeName.get(element.asType());
        String instanceName = Helper.toCamelCase(elementName);
        MethodSpec.Builder buildMethodBuilder = MethodSpec
                .methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .returns(elementType)
                .addStatement("$1T $2N = new $1T()", elementType, instanceName);
        for (FieldSpec fieldSpec : fieldSpecs) {
            buildMethodBuilder.addStatement("$1N.$2N = $2N", instanceName, fieldSpec);
        }
        buildMethodBuilder.addStatement("return $N", instanceName);
        MethodSpec buildMethod = buildMethodBuilder.build();
        //...
    }
```

然后创建TypeSpec对象并把前面这些属性啊setter啊build方法都添加进来：

```
    private TypeSpec createTypeSpec(Element element, ClassName builderClassName, String elementName) {
        //...
        return TypeSpec
                .classBuilder(builderClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addFields(fieldSpecs)
                .addMethods(setterSpecs)
                .addMethod(buildMethod)
                .build();
    }
```

最后创建JavaFile对象并写入到filer中：

```
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            //...
            TypeSpec typeSpec = createTypeSpec(element, builderClassName, elementName);

            JavaFile javaFile = JavaFile.builder(packageName, typeSpec).build();
            try {
                javaFile.writeTo(filer);
            } catch (IOException e) {
                onError("Failed to write java file: " + e.getMessage(), element);
            }
        }
        return true;
    }
```

## 4.使用

注解和注解处理器都写好了，接下来就非常简单。
我们先给app模块的`build.gradle`添加上依赖：

```
dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    implementation project(':annotation')
    annotationProcessor project(':processor')
}
```

接着给原来的**User**类添加上我们的自定义注解**@Builder**：

```
@Builder
public class User {
    // ...
}
```

然后我们在Android Studio的`Build`菜单栏选择`Make Project`，完了我们可以在`app/build/generated/source/apt/debug`文件夹下看到我们生成的`UserBuilder.java`源码。

接下来我们就可以在项目中直接使用生成的**UserBuilder**类了。

```
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        User me = new UserBuilder()
                .firstName("HeAn")
                .lastName("Zhu")
                .nickName("violet")
                .age(22)
                .build();
    }

}

```

## 5.参考
- [Ryan Harter: @Eliminate("Boilerplate")](https://academy.realm.io/posts/360andev-ryan-harter-eliminate-boilerplate/)
- [Droidcon NYC 2015 - @AnnotationProcessors ("ByExample")](https://www.youtube.com/watch?v=dBUAqPs0TB0)
- [ANNOTATION PROCESSING 101](http://hannesdorfmann.com/annotation-processing/annotationprocessing101)
- [The 10-Step Guide to Annotation Processing in Android Studio](https://stablekernel.com/the-10-step-guide-to-annotation-processing-in-android-studio/)
- [square/javapoet](https://github.com/square/javapoet)