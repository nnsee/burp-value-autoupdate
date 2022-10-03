## Burp Value Autoupdater

### Description
Simple Burp Suite plugin which stores values from incoming requests.

The values to watch for can be defined using regex or simply header names.

Values which have been stored can be used in outgoing requests using `$placeholders$`.

### Usage

As an example, let's say we want to keep track of a CSRF token, sent to us as the cookie `csrf`.

We set up a regex to watch for the value like so:

![Regex UI](https://nns.ee/vua/regex-ui.png)

We receive a response to a request (via any enabled tool) that contains a new CSRF value:
```
HTTP/1.1 200 OK
Set-Cookie: csrf=the_csrf_token
```

The stored value gets updated with the token we received in the response:

![Table view](https://nns.ee/vua/table-view.png)

We can then use the `$placeholder$` in a request, which will automatically fill in the stored value:
```http
GET / HTTP/1.1
Host: nns.ee
Cookie: csrf=$csrf$; session=123
```

Regex matching (and placing values) works in any part of the request, not just the headers.

The regex matcher uses the [re2 syntax](https://github.com/google/re2/wiki/Syntax).

### Installation

Currently, no `.jar` files are provided. Once I feel this project is polished enough to publish releases for, I will probably upload this to the Burp App store.

You can, however, [build the `.jar`](#building) yourself if you so desire.

Once you have a `.jar`, in Burp Suite, go to _Extender_ -> _Add_ and load the file as a Java extension.

### Building

Building is done via Gradle. To build a `.jar` with all dependencies included, do:

```
./gradlew shadowJar
```

The `.jar` file can then be found in `build/libs/` (look for the version tagged `-all`).