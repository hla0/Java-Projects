import java.io.*;
import java.util.*;
import java.util.concurrent.*;

// an RGB triple
class RGB {
	public int R, G, B;

	RGB(int r, int g, int b) {
		R = r;
		G = g;
		B = b;
	}

	public String toString() { return "(" + R + "," + G + "," + B + ")"; }

}

class GaussianTask extends RecursiveTask<RGB[]>{
	RGB[] source;
	int low;
	int high;
	int rowLength;
	double[][] gFilter;
	int radius;
	RGB[][] s;
	
	public GaussianTask(RGB[] src, int l, int h, int rLength, double[][] filter, int r, RGB[][] s1) {
		s = s1;
		source = src;
		low = l;
		high = h;
		rowLength = rLength;
		gFilter = filter;
		radius = r;
	}
	
	public RGB[] computeDirectly() {
		int curRow;
		int curCol;
		float curR;
		float curG;
		float curB;
		RGB[] temp = new RGB[high - low];
		
		for (int a = low; a < high; a++) {
			curRow = a/rowLength;
			curCol = a%rowLength;
			curR = 0;
			curG = 0;
			curB = 0;
			for (int b = -radius; b <= radius; b++) {
				for (int c = -radius; c <= radius; c++) {
					if (b < 0 && curRow + b < 0) {
						curRow = 0;
					}
					if (b > 0 && curRow + b >= source.length) {
						curRow = source.length - 1;
					}
					if (c < 0 && curCol + c < 0) {
						curCol = 0;
					}
					if (c > 0 && curRow + b >= source.length) {
						curRow = source.length - 1;
					}
					curR += gFilter[b][c] * s[curRow][curCol].R;
					curG += gFilter[b][c] * s[curRow][curCol].G;
					curB += gFilter[b][c] * s[curRow][curCol].B;
				}
			}
			temp[a - low].R = (int) curR;
			temp[a - low].G = (int) curG;
			temp[a - low].B = (int) curB;
		}
		return temp;
	}
	
	public RGB[] combineArr(RGB[] first, RGB[] second) {
		int fLen = first.length;
		RGB[] result = new RGB[fLen + second.length];
		for (int a = 0; a < fLen; a++) {
			result[a] = first[a];
		}
		for (int b = 0; b < second.length; b++) {
			result[fLen + b] = second[fLen + b];
		}
		return result;
	}
	
	protected RGB[] compute() {
		
		if (high - low < 5 * rowLength) {
			return computeDirectly();
		}
		int split = (high - low) / 2;
		split = (split/rowLength) * rowLength;

		GaussianTask m1 = new GaussianTask(source, low, low + split, rowLength,gFilter,radius,s);
		GaussianTask m2 =  new GaussianTask(source, low + split, high, rowLength,gFilter,radius,s);
		m1.fork();
		RGB[] second = m2.compute();
		RGB[] first = m1.join();
		return combineArr(first,second);
	}
}

class MirrorTask extends RecursiveTask<RGB[]>{
	RGB[] source;
	int low;
	int high;
	int rowLength;
	int height;
	public MirrorTask(RGB[] src, int l, int h, int rLength, int h1) {
		source = src;
		low = l;
		high = h;
		rowLength = rLength;
		height = h1;
	}

	public RGB[] computeDirectly() {
		RGB[] temp = new RGB[height * rowLength];
		for (int b = 0; b < height; b++) {
			for (int a = 0; a < rowLength; a++) {
				try {
					temp[b*rowLength + a] = source[low + (b+1)*rowLength - (a+1)];
				}
				catch (Exception e) {
					System.out.println(low + (b+1)*rowLength - (a+1));
					System.out.println("low: " + low + "\nhigh: " + high + "\nLength: " + source.length);
				}
			}
		}
		return temp;
	}
	
	public RGB[] combineArr(RGB[] first, RGB[] second) {
		int fLen = first.length;
		RGB[] result = new RGB[fLen + second.length];
		try {
			for (int a = 0; a < fLen; a++) {
				result[a] = first[a];
			}
			for (int b = fLen; b < second.length + fLen; b++) {
				result[b] = second[b - fLen];
			}
		}
		catch (Exception e) {
			System.out.println("Unable to combine " + result[0] + " " + result[fLen + second.length]);
		}
		return result;
	}
	
	protected RGB[] compute() {
		
		if (high - low < 5 * rowLength) {
			return computeDirectly();
		}

		MirrorTask m1 = new MirrorTask(source, low, low + (height/2)*rowLength, rowLength, height/2);
		MirrorTask m2 =  new MirrorTask(source, low + (height/2)*rowLength, high, rowLength,height - height/2);
		m1.fork();
		RGB[] first;
		RGB[] second;
		try {
			second = m2.compute();
			first = m1.join();
		}
		catch (Exception e) {
			System.out.println("Can't get separate arrays");
			first = new RGB[0];
			second = new RGB[0];
		}

		return combineArr(first,second);
	}
	
}
// code for creating a Gaussian filter
class Gaussian {

	protected static double gaussian(int x, int mu, double sigma) {
		return Math.exp( -(Math.pow((x-mu)/sigma,2.0))/2.0 );
	}

	public static double[][] gaussianFilter(int radius, double sigma) {
		int length = 2 * radius + 1;
		double[] hkernel = new double[length];
		for(int i=0; i < length; i++)
			hkernel[i] = gaussian(i, radius, sigma);
		double[][] kernel2d = new double[length][length];
		double kernelsum = 0.0;
		for(int i=0; i < length; i++) {
			for(int j=0; j < length; j++) {
				double elem = hkernel[i] * hkernel[j];
				kernelsum += elem;
				kernel2d[i][j] = elem;
			}
		}
		for(int i=0; i < length; i++) {
			for(int j=0; j < length; j++)
				kernel2d[i][j] /= kernelsum;
		}
		return kernel2d;
	}
}

// an object representing a single PPM image
class PPMImage {
	protected int width, height, maxColorVal;
	protected RGB[] pixels;

	PPMImage(int w, int h, int m, RGB[] p) {
		width = w;
		height = h;
		maxColorVal = m;
		pixels = p;
	}

	// parse a PPM file to produce a PPMImage
	public static PPMImage fromFile(String fname) throws FileNotFoundException, IOException {
		FileInputStream is = new FileInputStream(fname);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		br.readLine(); // read the P6
		String[] dims = br.readLine().split(" "); // read width and height
		int width = Integer.parseInt(dims[0]);
		int height = Integer.parseInt(dims[1]);
		int max = Integer.parseInt(br.readLine()); // read max color value
		br.close();

		is = new FileInputStream(fname);
		// skip the first three lines
		int newlines = 0;
		while (newlines < 3) {
			int b = is.read();
			if (b == 10)
				newlines++;
		}

		int MASK = 0xff;
		int numpixels = width * height;
		byte[] bytes = new byte[numpixels * 3];
		is.read(bytes);
		RGB[] pixels = new RGB[numpixels];
		for (int i = 0; i < numpixels; i++) {
			int offset = i * 3;
			pixels[i] = new RGB(bytes[offset] & MASK, bytes[offset+1] & MASK, bytes[offset+2] & MASK);
		}

		return new PPMImage(width, height, max, pixels);
	}

	// write a PPMImage object to a file
	public void toFile(String fname) throws IOException {
		FileOutputStream os = new FileOutputStream(fname);

		String header = "P6\n" + width + " " + height + "\n" + maxColorVal + "\n";
		os.write(header.getBytes());

		int numpixels = width * height;
		byte[] bytes = new byte[numpixels * 3];
		int i = 0;
		for (RGB rgb : pixels) {
			bytes[i] = (byte) rgb.R;
			bytes[i+1] = (byte) rgb.G;
			bytes[i+2] = (byte) rgb.B;
			i += 3;
		}
		os.write(bytes);
	}

	// implemented using Java 8 Streams
	public PPMImage negate() {
		RGB[] temp = new RGB[pixels.length];
		for (int i = 0; i < pixels.length; i++) {
			temp[i] = new RGB(pixels[i].R,pixels[i].G,pixels[i].B);
		}
		return new PPMImage(width,height,maxColorVal,Arrays.stream(temp)
			.parallel()
			.map(p -> {p.R = maxColorVal - p.R;p.G = maxColorVal - p.G;p.B = maxColorVal - p.B; return p;})
			.toArray(RGB[]::new));
	}

	// implemented using Java's Fork/Join library
	public PPMImage mirrorImage() {
		MirrorTask t = new MirrorTask(pixels,0,pixels.length,width,height);
		return new PPMImage(width,height,maxColorVal,t.compute());
	}

	// implemented using Java's Fork/Join library
	public PPMImage gaussianBlur(int radius, double sigma) {
		RGB[][] s = new RGB[width][height];
		for (int a = 0; a < pixels.length; a++) {
			s[a/width][a%width] = pixels[a];
		}
		GaussianTask t = new GaussianTask(pixels,0,pixels.length,width,Gaussian.gaussianFilter(radius, sigma),radius,s);
		return new PPMImage(width,height,maxColorVal,t.compute());
	}

	// implemented using Java 8 Streams
	public PPMImage gaussianBlur2(int radius, double sigma) {
		RGB[][] s = new RGB[width][height];
		for (int a = 0; a < pixels.length; a++) {
			s[a/width][a%width] = pixels[a];
		}
		RGB[] temp = new RGB[pixels.length];
		for (int i = 0; i < pixels.length; i++) {
			temp[i] = new RGB(pixels[i].R,pixels[i].G,pixels[i].B);
		}
		return new PPMImage(width,height,maxColorVal,Arrays.stream(temp)
			.parallel()
			.map(p -> { return p;})
			.toArray(RGB[]::new));
	}
}

//for testing
class test {
	public static void main(String[] args) {
		try {
			PPMImage im = new PPMImage(0,0,0,null);
			im = im.fromFile("florence.ppm");
			im.negate().toFile("neg.ppm");
			im.mirrorImage().toFile("mirror.ppm");
			im.gaussianBlur(5,2.0).toFile("gaussian1.ppm");
			im.gaussianBlur2(5,2.0).toFile("gaussian2.ppm");

			System.out.println("Test completed. Check your image file(s).\n");
		}
		catch (IOException e) {
			System.out.println("Error\n");
		}
	}
}

