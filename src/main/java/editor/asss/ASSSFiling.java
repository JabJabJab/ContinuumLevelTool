// Stan Bak
// ASSS Filing
// July 23rd, 2004
// This class provides static methods for loading and saving region files

package editor.asss;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

import javax.swing.JOptionPane;

import editor.loaders.BitMap;
import editor.loaders.LevelFile;

public class ASSSFiling
{
	/*
	 * public static void saveRegions(String filename, Vector regions) {
	 * BufferedWriter bw = null; Region region = null;
	 * 
	 * try { FileWriter fw = new FileWriter(filename); bw = new
	 * BufferedWriter(fw);
	 * 
	 * bw.write("asss region file version 1"); writeWindowsEndline(bw);
	 * bw.write(";file generated by Continuum Level / Ini Tool by 2dragons /
	 * Bak"); writeWindowsEndline(bw);
	 * 
	 * for (int x = 0; x < regions.size();++x) { Region r =
	 * (Region)regions.get(x);
	 * 
	 * writeWindowsEndline(bw); bw.write("Name: " + r.name);
	 * writeWindowsEndline(bw);
	 * 
	 * for (int c = 0; c< r.rects.size();++c) { bw.write("| " +
	 * getEnocdedRect((Rectangle)r.rects.get(c))); writeWindowsEndline(bw); }
	 * 
	 * if (r.isBase)) { bw.write("isbase"); writeWindowsEndline(bw); } }
	 * 
	 * bw.close(); } catch (IOException e) {
	 * JOptionPane.showMessageDialog(null,e); } }
	 */

	/**
	 * Load in regions from a .RGN file at the specified filename
	 * 
	 * @param filename
	 *            the path to the file that ends in .rgn
	 * @return the Vector of Regions to import, or null if errrored
	 */
	private static Vector loadRegionsFromRGNFile(String filename)
	{
		BufferedReader br = null;
		Region region = null;
		Vector rv = new Vector();
		String error = null;

		try
		{
			FileReader fr = new FileReader(filename);
			br = new BufferedReader(fr);

			if (br.ready())
			{
				String line = br.readLine().trim();
				if (!line.equals("asss region file version 1"))
				{
					error = "BAD FILE FORMAT: " + line
							+ " is not equal to \"asss region file version 1\"";
				}

			}

			while (br.ready() && error == null)
			{
				String line = br.readLine().trim();
				if (line == null)
					break;
				else if (line.length() == 0 || line.charAt(0) == ';')
					continue;
				else if (line.length() > 4
						&& line.substring(0, 4).toLowerCase().equals("name"))
				{
					if (region != null)
					{
						rv.add(region);
					}

					String name = line.substring(4).trim();
					if (name.length() == 0)
					{
						error = "line detected with just \"name\" and no actual region name.";
						continue;
					}
					if (name.charAt(0) == ':')
						name = name.substring(1).trim();

					String newName = name.replace(' ', '_');

					if (!newName.equals(name))
					{
						JOptionPane.showMessageDialog(null,
								"Spaces have been converted to underscores for region "
										+ newName);
						name = newName;
					}

					region = new Region(name, getColorForRow(rv.size()));
				}
				else if (line.length() >= 6 && line.equals("isbase"))
				{
					if (region == null)
					{
						error = "isbase detected with no region name: " + line;
						continue;
					}

					region.isBase = true;
				}
				else if (line.length() >= 10 && line.charAt(0) == '|')
				{
					if (region == null)
					{
						error = "rectangle dectected without region name: "
								+ line;
						continue;
					}

					line = line.substring(2);

					Rectangle r = decodeRect(line);

					if (r == null)
					{
						error = "rectangle can not be decoded properly: "
								+ line;
						continue;
					}

					region.rects.add(r);
				}
				else
				{
					error = "Unknown line: \"" + line + "\"";
				}
			}

			if (region != null)
			{
				// System.out.println("adding region " + region.name);
				rv.add(region);
			}

			br.close();
		}
		catch (IOException e)
		{
			JOptionPane.showMessageDialog(null, e);
		}

		// validate unique regions
		if (error == null)
		{
			for (int x = 0; x < rv.size(); ++x)
			{
				String name1 = ((Region) rv.get(x)).name;
				for (int y = x + 1; y < rv.size(); ++y)
				{
					String name2 = ((Region) rv.get(y)).name;

					if (name1.equals(name2))
					{
						error = "Non unique region name: " + name1;
						break;
					}
				}

				if (error != null)
					break;
			}
		}

		if (error != null)
		{
			JOptionPane.showMessageDialog(null, "ERROR: " + error);
			rv = null;
		}

		return rv;
	}

	/**
	 * Load in regions from a .LVL file at the specified filename
	 * 
	 * @param filename
	 *            the path to the file that ends in .lvl
	 * @return the Vector of regions to load, or null if errored
	 */
	private static Vector loadRegionsFromELVLFile(String filename)
	{
		File theFile = new File(filename);
		LevelFile loader;
		Vector rv = null;

		try
		{
			BufferedInputStream bs = new BufferedInputStream(
					new FileInputStream(theFile));
			BitMap bmp = new BitMap(bs);
			String error = null;
			bmp.readBitMap(false);

			if (!bmp.hasELVL)
			{
				error = "The .lvl does not contain any eLVL data (and therefore no regions either)!";
			}
			else
			{
				if (bmp.isBitMap())
				{
					loader = new LevelFile(theFile, bmp, true, bmp.hasELVL);
				}
				else
				{
					loader = new LevelFile(theFile, bmp, false, bmp.hasELVL);
				}
				error = loader.readLevel();

				if (error == null)
					rv = loader.regions;
			}

			if (error != null)
				JOptionPane.showMessageDialog(null, error);
			else
			{

			}

			bs.close();
		}
		catch (IOException e)
		{
			JOptionPane.showMessageDialog(null, e);
		}

		return rv;
	}

	/**
	 * Load regions from a .lvl or .rgn file
	 * 
	 * @param filename
	 *            the filename the user wants to load from
	 * @return a vector of loaded regions or null
	 */
	public static Vector loadRegions(String filename)
	{
		Vector rv = null;
		String smallPath = filename.toLowerCase();

		if (smallPath.endsWith(".rgn"))
		{ // load rgn regions
			rv = loadRegionsFromRGNFile(filename);

		}
		else if (smallPath.endsWith(".lvl"))
		{ // load elvl regions
			rv = loadRegionsFromELVLFile(filename);
		}
		else
		{
			JOptionPane
					.showMessageDialog(null,
							"Regions can only be imported from .rgn and .lvl(elvl) files.");
		}

		JOptionPane.showMessageDialog(null, (rv == null ? "0 regions" : rv
				.size()
				+ " region(s)")
				+ " imported.");

		return rv;
	}

	// line's length is at least 8 when this is called
	private static Rectangle decodeRect(String line)
	{
		int[] vals = new int[8];

		for (int x = 0; x < 8; ++x)
		{
			vals[x] = char_to_val(line.charAt(x));
			if (vals[x] == -1)
				return null;
		}

		return new Rectangle(vals[0] * 32 + vals[1], vals[2] * 32 + vals[3],
				vals[4] * 32 + vals[5], vals[6] * 32 + vals[7]);
	}

	/*
	 * private static String getEnocdedRect(Rectangle r) { String rv = "";
	 * 
	 * int first = r.x / 32; int second = r.x % 32;
	 * 
	 * rv += val_to_char(first); rv += val_to_char(second);
	 * 
	 * first = r.y / 32; second = r.y % 32;
	 * 
	 * rv += val_to_char(first); rv += val_to_char(second);
	 * 
	 * first = r.width / 32; second = r.width % 32;
	 * 
	 * rv += val_to_char(first); rv += val_to_char(second);
	 * 
	 * first = r.height / 32; second = r.height % 32;
	 * 
	 * rv += val_to_char(first); rv += val_to_char(second);
	 * 
	 * return rv; }
	 */

	/*
	 * private static char val_to_char(int val) { if (val >= 0 && val <= 25)
	 * return (char)(val + 'a'); else if (val >= 26 && val <= 31) return
	 * (char)(val + '1' - 26); else return (char)-1; }
	 */

	private static int char_to_val(char c)
	{
		if (c >= 'a' && c <= 'z')
			return c - 'a';
		else if (c >= '1' && c <= '6')
			return c - '1' + 26;
		else
			return -1;
	}

	public static Color getColorForRow(int row)
	{
		switch (row)
		{
		case 0:
			return (Color.red);

		case 1:
			return (Color.pink);

		case 2:
			return (Color.yellow);

		case 3:
			return (Color.green);

		case 4:
			return (Color.blue);

		case 5:
			return (Color.magenta);

		case 6:
			return (Color.cyan);

		case 7:
			return (Color.lightGray);
		}

		return (new Color(100 + ((row * 25) % 146), 81 + ((row * 132) % 165),
				119 + ((row * 37) % 127)));
	}

	/*
	 * private static void writeWindowsEndline(BufferedWriter bw) throws
	 * IOException { bw.write(System.getProperty( "line.separator" )); }
	 */
}