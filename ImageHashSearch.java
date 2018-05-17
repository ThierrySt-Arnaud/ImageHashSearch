// Perform search on an image database using perceptual hashing.
// Logs timing in CSV files in the folder the program is run from.

import java.io.*;
import java.util.*;

public class ImageHashSearch{
	//Set constants for program.
	final static int hashLength = 64;
	final static int imgLength = 65536;

	public static void main(String[] args) throws IOException{
		// Main program contains the database as well as the 
		// input scanner for user choices
		File imgdb = new File("./imgdb");
		List<File> images = pgmFiles(imgdb);
		Capillary<File> database = new Capillary<File>(hashLength);

		try(Scanner input = new Scanner(System.in)){
			// Prepare the database and time
			long startTime = System.nanoTime();
			prepareDB(database,images);
			long elapsed = System.nanoTime() - startTime;

			// The timing here will include parsing, hashing and insertion
			System.out.println("\033[2KDatabase of "+images.size()+" images prepared in "+elapsed+" ns.");

			// Call the search method
			imageSearch(database,input);
		}
	}	

	private static void prepareDB(Capillary<File> database, List<File> images) throws IOException{
		// Prepare timing file
		try(BufferedWriter InsertionTiming = new BufferedWriter(new FileWriter("InsertionTiming.csv"))){
			InsertionTiming.append("Insertion Time,Hash length = "+hashLength+",Image size = "+imgLength+",Number of images = "+images.size());
			InsertionTiming.newLine();
			// Then, for all images, open a stream
			for (File img : images){
				try(BufferedInputStream imgStrm = new BufferedInputStream(new FileInputStream(img))){
					System.out.print("\033[2K	Preparing " + img.getName() + '\r');

			   		if (imgStrm.read() != 80 || imgStrm.read() != 53) //Should get "P5" as first 2 reads
	   					throw new IllegalArgumentException("File is NOT raw PGM format.");

					BitSet hash = new BitSet(hashLength);
					
					// Parse then hash
					double[] parsedImg = parsePGM(imgStrm);
					Perceptual.Hash(parsedImg, hashLength, hash);
			
					// Insert and time
					long startTime = System.nanoTime();
					database.put(img,hash);
					long elapsed = System.nanoTime() - startTime;

					// Write timing to file. Timing is ONLY dependent on insertion.
					InsertionTiming.append(Long.toString(elapsed));
					InsertionTiming.newLine();

				// If a known exception occurs, we warn the user, but keep running
				} catch(FileNotFoundException e) {
					System.err.println(e.toString() + "Unable to find file \"" + img.getName() + '"');
				} catch(SecurityException e){
					System.err.println(e.toString() + "No access to file \"" + img.getName() + '"');
				} catch(IOException e){
					System.err.println(e.toString() + "Unable to read file \"" + img.getName() + '"');
				} catch(IllegalArgumentException e){
					System.out.println(e.getMessage() + img.getName());
				}
			}
		}
	}

	// Returns a list with all files that have the proper extension in the folder
	private static List<File> pgmFiles(File dir) {
		List<File> pgmFiles = new ArrayList<File>();
		for (File file : dir.listFiles()) {
			if (file.getName().endsWith(".pgm")) {
				pgmFiles.add(file);
			}
		}
		return pgmFiles;
	}

	private static double[] parsePGM(BufferedInputStream imgStrm) throws IllegalArgumentException, IOException{
		imgStrm.mark(1); //We remember this place
   		while(imgStrm.read() == 10){ // If we got a newline
   			imgStrm.mark(4);
   			if(imgStrm.read() == 35){ // Then a comment
   				int readByte = imgStrm.read();
   				while(readByte != 10) {
   					imgStrm.mark(2);
   					readByte = imgStrm.read(); //Dump comments
   				}
   				imgStrm.reset(); // Then go back to whatever wasn't a new line
   			}
   			else{ //If it's not a comment
   				imgStrm.reset(); // We go back to the newline
   				break;
   			}
   		}
   		imgStrm.reset(); // Go back to previous mark
   		imgStrm.mark(10); // Remember this place
   		int lwidth = 0;
   		while (imgStrm.read() != 32) lwidth++; //Count until blankspace
   		imgStrm.reset(); //Go back to previous mark
		int width = 0;
		for (int i = 0; i < lwidth; i++){
			byte readByte = (byte) imgStrm.read();
   			if (readByte < 48 || readByte > 57)
   				throw new IllegalArgumentException("Invalid file format.");
			width += (readByte-48)*Math.pow(10,(lwidth-1-i)); // Convert ASCII to integer
		}// We now have width

   		imgStrm.read(); // Dump blankspace
   		imgStrm.mark(10); // Remember this place
		int lheight = 0;
   		while (imgStrm.read() != 10) lheight++; // Count until newline
   		imgStrm.reset();// Go back to previous mark
		int height = 0;
		for (int i = 0; i < lheight; i++){
			byte readByte = (byte) imgStrm.read();
			if (readByte < 48 || readByte>57)
				throw new IllegalArgumentException("Invalid file format.");
			height += (readByte-48)*Math.pow(10,(lheight-1-i)); // Convert ASCII to integer
		}// We now have height

		int size = height*width; // Find total size of image
		if (size != imgLength || height != width)
			throw new IllegalArgumentException("Invalid image size.");
		imgStrm.read(); // Dump newline
		imgStrm.mark(10); // Remember this place 
		int ldepth = 0;
		while (imgStrm.read() != 10) ldepth++; // Count until newline
		imgStrm.reset(); // Go black to previous mark
		int depth = 0;
		for (int i = 0; i < ldepth; i++){
			byte readByte = (byte) imgStrm.read();
			if (readByte < 48 || readByte>57)
				throw new IllegalArgumentException("Invalid file format.");
			depth += (readByte-48)*Math.pow(10,(ldepth-1-i)); //Convert ASCII to integer
		}
		if (depth != 255) // Then check if number of greys is good
			throw new IllegalArgumentException("Invalid grey depth.");
		double[] parsed = new double[size];
		for(int i = 0; i < size; i++){
			parsed[i] = imgStrm.read(); //Convert bytes to double for DCT
		}
		return parsed;
	}

	private static void imageSearch(Capillary<File> database, Scanner input) throws IOException{
		// Overwrite or create a file to store timing data
		try(BufferedWriter SearchTiming = new BufferedWriter(new FileWriter("SearchTiming.csv"))){
			SearchTiming.append("Hashing Time,Searching Time,Tolerance,Hits,Hash length = "+hashLength+
								",Image size = "+imgLength+",Number of images = "+database.getSize());
			SearchTiming.newLine();

			// Run searches until user wants to quit
			while(true){
				System.out.println("Input path of the image or directory to search for or 'quit':");
				String comparePath = input.nextLine();
				if (comparePath.toLowerCase().equals("quit"))
					return;
				File toCompare = new File(comparePath);

				// If path is a directory
				if (toCompare.isDirectory()){
					List<File> images = pgmFiles(toCompare);
					// and contains pgm images
					if (images.isEmpty())
						System.out.println("This is a directory but contains no readable images.");
					else{ // Get tolerance to use on all valid files in directory
						System.out.println("This directory contains "+images.size()+" PGM images.");
						int tolerance = getTolerance(input);
						// Then attempt to find a match for each in the database
						for(File img : images){
							matchImages(database, img, tolerance, input, SearchTiming);
							System.out.print('\n');
						}
					}
				}// Otherwise call the match for the single file
				else
					matchImages(database, toCompare, -1, input, SearchTiming);
			}
		}
	}

	// Asks user for the percentage of tolerance
	private static int getTolerance(Scanner input){
		int tolerance = -1;
		while(tolerance < 0 || tolerance > 100){
			try{
				System.out.println("Enter the tolerance as percentage:");
				tolerance = input.nextByte();
				if (tolerance < 0 || tolerance > 100)
					System.out.println("Invalid tolerance value.");
				input.nextLine();
			}catch(InputMismatchException e){
				System.out.println("Invalid value format.");
				input.nextLine();
			}
		}
		return tolerance;
	}

	private static void matchImages(Capillary<File> database, File img, int tolerance, Scanner input,
									BufferedWriter SearchTiming) throws IOException{
		BitSet hashCompare = new BitSet(hashLength);
		double[] parsedCompare = new double[imgLength];
		boolean fileHashed = false;
		// First, we check if we can parse the file
		try (BufferedInputStream imgStrm = new BufferedInputStream(new FileInputStream(img))){
			System.out.print("	Parsing "+img.getName()+'\r');
	   		if (imgStrm.read() != 80 || imgStrm.read() != 53) //Should get "P5" as first 2 reads
				throw new IllegalArgumentException("File is NOT raw PGM format.");
			//and time the amount it took
			long startTime = System.nanoTime();
			parsedCompare = parsePGM(imgStrm);
			long elapsed = System.nanoTime() - startTime;
			System.out.println("\033[2KParsed "+img.getName()+" in "+elapsed+" ns.");

			// Once its parse, we hash it and time that
			System.out.print("	Hashing "+img.getName()+'\r');
			startTime = System.nanoTime();
			Perceptual.Hash(parsedCompare, hashLength, hashCompare);
			elapsed = System.nanoTime() - startTime;

			// Then we write the timing to file and flag the file as hashed
			SearchTiming.append(Long.toString(elapsed)+',');
			System.out.println("\033[2KHashed "+img.getName()+" in "+elapsed+" ns.");
			fileHashed = true;
			// If a known exception occurs, we warn the user, but keep running
		} catch(FileNotFoundException e) {
			System.out.println(e.toString() + " Unable to find file \"" + img.getName() + '"');
		} catch(SecurityException e){
			System.out.println(e.toString() + " No access to file \"" + img.getName() + '"');
		} catch(IOException e){
			System.out.println(e.toString() + " Unable to read file \"" + img.getName() + '"');
		} catch(IllegalArgumentException e){
			System.out.println(e.getMessage() + img.getName());
		}

		// If the file was hashed and parsed successfully
		if(fileHashed){
			// We ask the user to set the tolerance, if necessary
			if(tolerance < 0)
				tolerance = getTolerance(input);
			// Then we search and time that
			long startTime = System.nanoTime();
			List<File> matches = database.search(hashCompare,((tolerance*hashLength)/100));
			long elapsed = System.nanoTime() - startTime;

			// And we output the names of the matches, if any
			if (matches.isEmpty())
				System.out.println("No matches in the database.");
			else{
				Collections.<File>sort(matches);
				System.out.println("Matches with tolerance "+tolerance+"% are:");
				for (File match : matches){
					System.out.println(match.getName());
				}
			}

			// Then we display and write the timings
			System.out.println("Search took "+elapsed+" ns.");
			SearchTiming.append(Long.toString(elapsed)+','+Integer.toString(tolerance)+','+Integer.toString(matches.size()));
			SearchTiming.newLine();
		}
	}
}
