icobundle
---------
Copyright 2003 Toby Thain <toby@telegraphics.com.au>.


DESCRIPTION

The "icobundle" utility copies Windows ICO format images from multiple
input files, and bundles them together into a single output file, in ICO format. 
This program is meant to accompany my Photoshop ICO file format plugin,
also released under the GPL and available from http://www.telegraphics.com.au/sw/

Source code is supplied (under GPL) to build under Linux 
and probably other UNIX variants. Built executables are supplied 
for Mac OS (PowerPC and 68K).

(For more information on ICO format, see
http://msdn.microsoft.com/library/default.asp?url=/library/en-us/dnwui/html/msdn_icons.asp )

Please send comments and bug reports to support@telegraphics.com.au


TO USE (Mac version)

Simply drag the ICO files you wish to bundle on to the application.
Name the output file and OK. The icons will be stored as a suite in the output file.

TO USE (Command line version)

The syntax is simply:
	./icobundle -o outputfile inputfile(s)

While icobundle is running, some text is printed explaining what's going on.
icobundle will report any problems it encounters. Some integrity checks are done
on input files.


TO BUILD (Command line tool)

"icobundle" has been tested under Linux and OS X, although it should build
under most varieties of UNIX (BSD "make" may have problems with the Makefile
as distributed - e.g. NetBSD).

As distributed, the command line tool cannot be used with Windows 
unless an environment such as Cygwin or MinGW is used - 


Under Mac OS X:
* the Developer Tools must first be installed.
* do not expand the source archive with Stuffit Expander,
it will not fully recreate the source tree. Use the commands below.

To build the program, put the source archive in the current directory
and execute the following commands in Terminal or a shell.

		mkdir icobundle
		cd icobundle
		tar -xzf ../icobundle-1.1b1-GPL-src.tar.gz

		make

To build Windows "console" tools with MinGW hosted on Linux,
extract the source archive using the first three lines above, then:

		CC=mingw32-gcc make
		mv icocheck icocheck.exe
		mv icobundle icobundl.exe

This builds two executables, "icocheck" and "icobundl",
which can be copied to Windows used on the MS-DOS command line.

For more information on building for Windows,
see http://www.cygwin.com/ and http://www.mingw.org/


TO BUILD (Mac OS application)

After extracting the source, the directory layout should be:

icobundle/
	dist/
		ICOBundle/
			Contents/
				MacOS/
				MacOSClassic/
				PkgInfo
	...files...
common/
	tt/
		...files...
icoformat/
	...files...

In MPW, change the current directory to the "icobundle" directory.
Choose "Build Program", enter "icobundle", and hit OK.
This should build the two PowerPC binaries (Classic and Carbon) 
as an OS X bundle, "ICOBundle", and the 68K binary, "ICOBundle.68K".
