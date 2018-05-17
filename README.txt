Image Hash Search README


----====####====---- What is this? ----====####====----

	This software is an image comparator that tries to find similar
	images in a database within a specified tolerance from a given
	file or directory. It uses a DCT to hash the images and a bitwise
	trie to compare the hash with the database. For more details, you
	can take a look at the more detailed project report.

----====####====---- Compatibility notes ----====####====----

	This software was tested on both Mac OS X 10.12.6 and Windows 10
	with the Fall Creators update, both on JRE 1.8. While it works 
	fine on Windows, the command line might display some weird things
	at certain places. Additionally, the high volume of lines displayed
	when doing a directory search might not be displayed entirely on
	the small buffer of the Windows Command Line.

----======#######======---- Usage ----=====#######=====----

	The software can ONLY read PGM files that are
		-Raw type
		-Grey depth of 255
		-256x256

	A GIMP script is included to convert almost any file type to the
	required format. It must be placed in the GIMP scripts directory.
	Once placed, open Gimp -> File -> Create -> bath-image-hash-format
	A window will ask you for the directory of the source images and the
	desination directory. It might take some time.

	Only PGM files created in GIMP have been tested. Others should be fine
	as long as they use the newline (ASCII 10) and blank spaces (ASCII 32)
	for the delimiters.

	To be included in the search database, images MUST be placed inside
	the imgdb folder. The images to search for can be on any mounted drive
	on your computer. They must still have a valid format, however.

	When the program asks for a path, it must NOT be placed in quotation
	marks or have the spaces escaped. Paths can be relative or absolute.

	Examples:
	To search the whole database you can simply enter
		imgdb
	or
		.\imgdb
	or
		C:\\Path\leading to the\program\files\imgdb

	To search for a specific file within the database you could enter
		imgdb\nameoftheimage.pgm

	On a Mac OS or Linux machine, paths follow standard Unix notation.

----====####====---- And finally… ————====####====----

A big thank you to Project Nayuki for the wonderful fast DCT and FFT algorithms in Java!


