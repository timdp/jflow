JFlow
=====

**JFlow** implements a *Cover Flow*-like user interface in *Java Swing*. A
[demo](http://timdp.github.io/jflow/) is available.

Quick Usage Instructions
------------------------

Sample code is available in the `be.pwnt.jflow.demo` package. Generally, you
would probably use JFlow as follows:

1. Get or compile the JFlow JAR file and add it to your class path.
2. Look at the source code of `be.pwnt.jflow.Configuration`, which contains all
the available parameters.
3. Create an instance of `Configuration`. At the very least, create the shape
array and fill it with instances of `be.pwnt.jflow.shape.Picture`.
4. Create an instance of `JFlowPanel`, passing it the `Configuration` instance.
5. Use the `JFlowPanel` instance like any other `JPanel`.

Interactivity
-------------

Three shape-related events are currently implemented:

* activating a shape (moving the cursor over it),
* deactivating a shape (moving the cursor off it), and
* clicking an activated shape.

To capture such events, `JFlowPanel` offers a standard Observer pattern.
Implement `be.pwnt.jflow.ShapeListener` in a new class, instantiate the class 
and register the instance with the panel using the `addListener` method. This
procedure is also used in the `be.pwnt.jflow.demo` package.

Author
------

[Tim De Pauw](http://tmdpw.eu/)

License
-------

This program is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation, either version 3 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see <http://www.gnu.org/licenses/>.
