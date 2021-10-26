![version: 0.6.0](https://img.shields.io/badge/kobweb-v0.6.0-yellow)
<a href="https://discord.gg/5NZ2GKV5Cs">
  <img alt="Varabyte Discord" src="https://img.shields.io/discord/886036660767305799.svg?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2" />
</a>
[![Follow @bitspittle](https://img.shields.io/twitter/follow/bitspittle.svg?style=social)](https://twitter.com/intent/follow?screen_name=bitspittle)

# K🕸️bweb

```kotlin
@Page
@Composable
fun HomePage() {
  Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
    Row(Modifier.align(Alignment.End)) {
      var colorMode by rememberColorMode()
      Button(
        onClick = { colorMode = colorMode.opposite() },
        Modifier.clip(Circle())
      ) {
        Box(Modifier.padding(4.dp)) {
          if (colorMode.isLight()) FaSun() else FaMoon()
        }
      }
    }
    H1 {
      Text("Welcome to Kobweb!")
    }
    Row {
      Text("Create rich, dynamic web apps with ease, leveraging ")
      Link("https://kotlinlang.org/", "Kotlin")
      Text(" and ")
      Link("https://compose-web.ui.pages.jetbrains.team/", "Web Compose")
    }
  }
}
```

<p align="center">
<img src="https://github.com/varabyte/media/raw/main/kobweb/screencasts/kobweb-welcome.gif" />
</p>

---

Kobweb is an opinionated Kotlin framework for building websites and web apps, inspired by [Next.js](https://nextjs.org)
and [Chakra UI](https://chakra-ui.com).

**It is currently in technology preview**. Please consider starring the project to indicate interest, so we know we're
creating something the community wants. [How ready is it?](https://github.com/varabyte/kobweb#can-we-kobweb-yet)



Our goal is to provide:

* an intuitive structure for organizing your Kotlin website or web app
* automatic handling of routing between pages
* a collection of useful _batteries included_ widgets built on top of Web Compose
* an environment built from the ground up around live reloading
* static site exports for improved SEO
* shared, rich types between client and server
* out-of-the-box Markdown support
* an open source foundation that the community can extend
* and much, much more!

Here's a demo where we create a Web Compose website from scratch with Markdown support and live reloading, in under 10
seconds:

https://user-images.githubusercontent.com/43705986/135570277-2d67033a-f647-4b04-aac0-88f8992145ef.mp4

# Trying it out yourself

## Build the Kobweb binary

**Note:** Building Kobweb requires JDK11 or newer. If you don't already have this set up, the easiest way is to
[download a JDK](https://docs.aws.amazon.com/corretto/latest/corretto-11-ug/downloads-list.html), unzip it somewhere,
and update your `JAVA_HOME` variable to point at it.

```bash
JAVA_HOME=/path/to/jdks/corretto-11.0.12
# ... or whatever version or path you chose
```

Once the code stabilizes a bit, we will host an artifact for downloading, but it's easy enough to build your own for
now.

```bash
$ cd /path/to/src/root
$ git clone --recurse-submodules https://github.com/varabyte/kobweb
$ cd kobweb
$ ./gradlew :cli:kobweb:installDist
```

I recommend putting Kobweb in your path:

```bash
$ PATH=$PATH:/path/to/src/root/kobweb/cli/kobweb/build/install/kobweb/bin
$ kobweb version
```

## Create your Kobweb Site

```bash
$ cd /path/to/projects/root
$ kobweb create site
```

You'll be asked a bunch of questions required for setting up your project. When finished, you'll have a basic project
with three pages - a home page, an about page, and a markdown page - and some components (which are collections of
reusable, composable pieces). Your own directory structure should look something like:

```
my-project
└── src
    └── jsMain
        ├── kotlin
        │   └── org
        │       └── example
        │           └── myproject
        │               ├── components
        │               │  ├── layouts
        │               │  │  └── PageLayout.kt
        │               │  ├── sections
        │               │  │  └── NavHeader.kt
        │               │  └── widgets
        │               │     └── GoHomeLink.kt
        │               ├── MyApp.kt
        │               └── pages
        │                   ├── About.kt
        │                   └── Index.kt
        └── resources
            └── markdown
                └── Markdown.md

```

Note that there's no index.html or routing logic anywhere! We generate that for you automatically when you run Kobweb.
Which brings us to the next section...

## Run your Kobweb site

```bash
$ cd /path/to/projects/root/your-project
$ kobweb run
```

This command spins up a webserver at http://localhost:8080. If you want to configure the port, you can do so by editing
your project's `.kobweb/conf.yaml` file.

You can open your project in IntelliJ and start editing it. While Kobweb is running, it will detect changes, recompile,
and deploy updates to your site automatically.

# Basics

Kobweb, at its core, is a handful of classes responsible for trimming away much of the boilerplate around building a
Web Compose app, such as routing and setting up default CSS styles. It exposes a handful of annotations and utility
methods which your app can use to communicate intent with the framework. These annotations work in conjunction with our
Gradle plugin (`com.varabyte.kobweb.application`) that handles code and resource generation for you.

Kobweb is also a CLI binary of the same name which provides commands to handle the parts of building a Web Compose app
that are less glamorous. We want to get that stuff out of the way, so you can enjoy focusing on the more interesting
work!

## Create a page

Creating a page is easy! It's just a normal `@Composable` method. To upgrade your composable to a page, all you need to
do is:

1. Define your composable in a file somewhere under the `pages` package
1. Annotate it with `@Page`

Just from that, Kobweb will create a site entry for you automatically.

For example, if I create the following file:

```kotlin
// com/example/mysite/pages/admin/Settings.kt

@Page
@Composable
fun SettingsPage() {
    /* ... */
}
```

this will create a page that I can then visit by going to `mysite.com/admin/settings`.

By default, the path comes from the file name, although there will be ways to override this behavior on a
case-by-case basis (* *coming soon*).

The file name `Index.kt` is special. If a page is defined inside such a file, it will be treated as the default page
under that URL. For example, a page defined in `.../pages/admin/Index.kt` will be visited if the user visits
`mysite.com/admin`.

## Silk

Silk is a UI layer included with Kobweb and built upon Web Compose. (To learn more about Web Compose, please visit
[the official tutorials](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Web/Getting_Started)).

While Web Compose requires you to understand underlying html / css concepts, Silk attempts to abstract a lot of that away,
providing an API more akin to what you might experience developing a Compose app on Android or Desktop. Less
"div, span, flexbox, attrs, styles, classes" and more "Rows, Columns, Boxes, and Modifiers".

We consider Silk a pretty important part of the Kobweb experience, but it's worth pointing out that it's designed as an
optional component. You can absolutely use Kobweb without Silk. You can also interleave Silk and Web Compose without
issue (as Silk, itself, is just composing Web Compose methods).

### What about Multiplatform Widgets?

Jetbrains is working on an experimental project called "multiplatform widgets" which is supposed to bring the Desktop /
Android API to the web. And it may seem like the Kobweb + Silk approach is competing with it.

However, I've found there is a fundamental distance between Desktop / Android Compose and Web Compose. Specifically,
Desktop / Android targets render to their own surface, while Web modifies a parallel html / css DOM tree and leaves it
to do the final rendering.

This has major implications on how similar the two APIs can get. For example, in Desktop / Android, the order you apply
modifiers matters, while in Web, this action simply sets html style properties under the hood, where order does not
matter.

One approach would be to own the entire rendering pipeline, ditching html / css entirely and targeting a full page
canvas or something. However, this limits the ability for robots to crawl and index your site, which is a major
drawback. It also means that debugging in a browser would be a rough experience, as the browser's developer tools would
be limited in the insights it could provide to your site. It would also prevent a developer from making use of the rich
ecosystem of Javascript libraries out there that modify the DOM tree themselves.

For now, I am making a bet that the best way forward is to embrace the web, sticking to html / css, but providing a
rich UI library of widgets that hopefully makes it relatively rare for the developer to worry about it. For example,
flexbox is a very powerful component, but you'll find it's much easier to compose Rows and Columns together than trying
to remember if you should be justifying your items or aligning your content, even if Rows and Columns are just creating
the correct html / css for you behind the scenes anyways.

## Components: Layouts, Sections, and Widgets

Outside of pages, it is common to create reusable composable parts. While Kobweb doesn't enforce any particular rule
here, we recommend a convention which, if followed, may make it easier to allow new readers of your codebase to get
around. 

First, as a sibling to pages, create a folder called **components**. Within it, add:

* **layouts** - High-level composables that provide entire page layouts. Most (all?) of your `@Page` pages will start 
  by calling a page layout function first. You may only have a single layout for your entire site.
* **sections** - Medium-level composables that represent compound areas inside your pages, organizing a collection of
  many children composables. If you have multiple layouts, it's likely sections would be shared across them. For
  example, nav headers and footers are great candidates for this subfolder. 
* **widgets** - Low-level composables. Focused UI pieces that you may want to re-use all around your site. For example,
  a stylized visitor counter would be a good candidate for this subfolder. 

## Examples

Kobweb will provide a growing collection of samples for you to learn from. To see them all, run:

```bash
$ kobweb list

You can create the following Kobweb projects by typing `kobweb create ...`

• site: A template for a minimal site that demonstrates the basic features of Kobweb
• examples/todo: An example TODO app, showcasing client / server interactions
```

All examples will live in the projects that start with the name "examples/". For example, `kobweb create examples/todo`
will instantiate a simple TODO app with a simple client and server setup that you can learn from.

# Can We Kobweb Yet

Current state: **Functional but early**

Kobweb has some pretty big pieces working already. It is easy to set up a new project and get things running quickly.
The live reloading flow is pretty nice, and you'll miss it when you switch to projects that don't have it. It supports
generating pages from Markdown that can reference your Composable code. And while it's not quite server-side rendering,
you can export static pages which will get hydrated on load.

However, there's still a lot to do. The API surface is a bit lean in some areas right now, especially around Silk UI
components, plus filling in holes in the APIs that interact with Web Compose. There are probably quite a few sharp
corners. And while the code is decently documented, higher level documentation is missing. Windows support needs
love.

So, should you use Kobweb at this point? If you are...

* a Kotlin tinkerer who is excited to play around with new tech, wants to have a voice in the direction of this project,
  and who isn't afraid of creating toy projects atop some APIs which may shift underfoot:
  * **YES!!!** Please see the [connecting with us](https://github.com/varabyte/kobweb#connecting-with-us) section below,
  we'd definitely love to hear from you.
* a Kotlin developer who wants to write a small web app or create a new blog from scratch:
  * ***Maybe***, but now is probably a bit too early.
* someone who already has an existing project in progress and wants to integrate Kobweb into it:
  * **No**
* a company:
  * **NOOOOOO** (someday, we hope, but not yet)
 
# Templates

Kobweb provides its templates in a separate git repository, which is referenced within this project as a submodule for
convenience. To pull down everything, run:

```bash
/path/to/src/root
$ git clone --recurse-submodules https://github.com/varabyte/kobweb

# or, if you've already previously cloned kobweb...
/path/to/src/root/kobweb
$ git submodule update --init
```

# Known Issues

* `kobweb run` sometimes gets stuck when Gradle (running behind it) gets stuck.
  * Quit kobweb, run `./gradlew --stop`, and then try again
  * Run `./gradlew kobwebRun` with various Gradle debug options to see what's going on under the hood

Solutions didn't work? Or you're encountering issues not listed here? Please consider
[filing a bug](https://github.com/varabyte/kobweb/issues/new/choose)!

# Connecting with us

* [Join our Discord!](https://discord.gg/5NZ2GKV5Cs)
* Follow me on Twitter: [@bitspittle](https://twitter.com/bitspittle)

# Filing issues and leaving feedback

It is still early days, and while we believe we've proven the feasibility of this approach at this point, there's still
plenty of work to do to get to a 1.0 launch! We are hungry for the community's feedback, so please don't hesitate to:

* [Open an issue](https://github.com/varabyte/kobweb/issues/new/choose)
* Contact us (using any of the ways mentioned above) telling us what features you want
* Ask us for guidance, especially as there are no tutorials yet (your questions can help us know what to write first!)

Thank you for your support and interest in Kobweb!