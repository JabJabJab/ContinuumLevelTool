// Stan Bak
// ASSS Region class file
// July 23rd, 2004

package editor.asss;

import editor.loaders.BitmapSaving;
import editor.loaders.ByteArray;

import java.awt.*;
import java.util.ArrayList;
import java.util.Vector;

/**
 * This class represents a region defined by several things: name - rNam isBase - rBSE isNoFlags -
 * rNFL isNoWeps - rNWP isNoAnti - rNAW isAutoWarp - rAWP
 *
 * <p>autoWarp x, y, and arena (in case of autowarp) arena is max 15 letters
 *
 * <p>vector of rectangles - rTIL
 *
 * @author baks
 */
public class Region {

  private static final int SMALL_EMPTY_RUN = 0;
  private static final int LONG_EMPTY_RUN = 1;
  private static final int SMALL_PRESENT_RUN = 2;
  private static final int LONG_PRESENT_RUN = 3;
  private static final int SMALL_EMPTY_ROWS = 4;
  private static final int LONG_EMPTY_ROWS = 5;
  private static final int SMALL_REPEAT = 6;
  private static final int LONG_REPEAT = 7;

  /** Region bytes loaded... but unknown or unused by the program. */
  private Vector<Byte> unknownBytes = new Vector<>();

  java.util.List<Rectangle> rects = new ArrayList<>();

  public Color color;

  public String name;
  String arena = "";

  /** Default auto-warp 'x' coordinate. */
  public int x = 512;
  /** Default auto-warp 'y' coordinate. */
  public int y = 512;

  boolean isBase = false;
  boolean isNoFlags = false;
  boolean isNoWeps = false;
  boolean isNoAnti = false;
  boolean isAutoWarp = false;

  public Region() {
    name = "@THIS_IS_A_BUG->ERROR"; // the user should never see this
    color = getRandomColor();
  }

  public Region(String newName, Color newColor) {
    name = newName;
    color = newColor;
  }

  /**
   * Get the encoding for this region, save the header
   *
   * @return a Vector of Bytes representing this region
   */
  public Vector getEncodedRegion() {
    Vector<Byte> encoding = new Vector<>();
    // encode isBase
    if (isBase) {
      encoding.add((byte) 'r');
      encoding.add((byte) 'B');
      encoding.add((byte) 'S');
      encoding.add((byte) 'E');
      encoding.add((byte) 0);
      encoding.add((byte) 0);
      encoding.add((byte) 0);
      encoding.add((byte) 0);
    }
    // encode noFlags
    if (isNoFlags) {
      encoding.add((byte) 'r');
      encoding.add((byte) 'N');
      encoding.add((byte) 'F');
      encoding.add((byte) 'L');
      encoding.add((byte) 0);
      encoding.add((byte) 0);
      encoding.add((byte) 0);
      encoding.add((byte) 0);
    }
    // encode noWeps
    if (isNoWeps) {
      encoding.add((byte) 'r');
      encoding.add((byte) 'N');
      encoding.add((byte) 'W');
      encoding.add((byte) 'P');
      encoding.add((byte) 0);
      encoding.add((byte) 0);
      encoding.add((byte) 0);
      encoding.add((byte) 0);
    }
    // encode noAnti
    if (isNoAnti) {
      encoding.add((byte) 'r');
      encoding.add((byte) 'N');
      encoding.add((byte) 'A');
      encoding.add((byte) 'W');
      encoding.add((byte) 0);
      encoding.add((byte) 0);
      encoding.add((byte) 0);
      encoding.add((byte) 0);
    }
    // encode isAutoWArp
    if (isAutoWarp) {
      encoding.add((byte) 'r');
      encoding.add((byte) 'A');
      encoding.add((byte) 'W');
      encoding.add((byte) 'P');
      if (!arena.equals("")) { // encode with arena, size = 20
        byte[] dword = BitmapSaving.toDWORD(20);
        for (int c = 0; c < 4; ++c) encoding.add(dword[c]);

        // save x
        byte[] word = BitmapSaving.toWORD(x);

        for (int c = 0; c < 2; ++c) encoding.add(word[c]);

        // save y
        word = BitmapSaving.toWORD(y);

        for (int c = 0; c < 2; ++c) encoding.add(word[c]);

        // save arena
        if (arena.length() > 15) arena = arena.substring(15);

        int len = arena.length();
        for (int c = 0; c < len; ++c) encoding.add((byte) arena.charAt(c));

        for (int c = len; c < 16; ++c) encoding.add((byte) 0);

      } else { // no arena, size = 4
        byte[] dword = BitmapSaving.toDWORD(4);
        for (int c = 0; c < 4; ++c) encoding.add(dword[c]);

        // save x
        byte[] word = BitmapSaving.toWORD(x);

        for (int c = 0; c < 2; ++c) encoding.add(word[c]);

        // save y
        word = BitmapSaving.toWORD(y);

        for (int c = 0; c < 2; ++c) encoding.add(word[c]);
      }
    }
    // encode unknown bytes
    encoding.addAll(unknownBytes);

    // encoded name
    encoding.add((byte) 'r');
    encoding.add((byte) 'N');
    encoding.add((byte) 'A');
    encoding.add((byte) 'M');

    int len = name.length();
    byte[] dword = BitmapSaving.toDWORD(len);
    for (int c = 0; c < 4; ++c) encoding.add(dword[c]);

    for (int c = 0; c < len; ++c) {
      encoding.add((byte) name.charAt(c));
    }

    // pad it
    int padding = 4 - len % 4;
    if (padding != 4) {
      for (int c = 0; c < padding; ++c) {
        encoding.add((byte) 0);
      }
    }

    // encode tiles
    encoding.add((byte) 'r');
    encoding.add((byte) 'T');
    encoding.add((byte) 'I');
    encoding.add((byte) 'L');

    // we now need the length! yuck! ok let's make another vector containing
    // just the encoding
    Vector<Byte> tileData = getCompressedRGN();

    dword = BitmapSaving.toDWORD(tileData.size());
    for (int c = 0; c < 4; ++c) encoding.add(dword[c]);

    encoding.addAll(tileData);

    // pad it
    padding = 4 - tileData.size() % 4;
    if (padding != 4) {
      for (int c = 0; c < padding; ++c) {
        encoding.add((byte) 0);
      }
    }

    return encoding;
  }

  /**
   * Get the compressed tiledata as a vector of Bytes
   *
   * @return the vector of bytes represting the encoding of this tiledata
   */
  private Vector<Byte> getCompressedRGN() {
    Vector<Byte> bytes = new Vector<>();

    boolean[][] rgn = new boolean[1024][1024];

    // start empty
    for (int y = 0; y < 1024; ++y) for (int x = 0; x < 1024; ++x) rgn[y][x] = false;

    // add rectangles
    for (Object rect : rects) {
      Rectangle r = (Rectangle) rect;

      int endX = r.x + r.width;
      int endY = r.y + r.height;

      for (int y = r.y; y < endY; ++y)
        for (int x = r.x; x < endX; ++x) {
          rgn[y][x] = true;
        }
    }

    Vector lastRow = null;
    int lastRowSameCount = 0;
    int emptyRowCount = 0;

    // okay now we actually have to do the encoding... row by row
    for (int curRow = 0; curRow < 1024; ++curRow) {
      int curY = 0;

      boolean emptyRow = true;
      for (; curY < 1024; ++curY) // check for empty row
      {
        if (rgn[curRow][curY]) {
          emptyRow = false;
          break;
        }
      }

      if (emptyRow) {
        emptyRowCount++;

        if (lastRowSameCount > 0) {
          bytes.addAll(encodeRepeatLastRow(lastRowSameCount));
        }

        lastRow = null;
        lastRowSameCount = 0;

        if (curRow == 1023) // end, encode it
        {
          bytes.addAll(encodeEmptyRows(emptyRowCount));
        }

        continue;
      }

      if (emptyRowCount > 0) { // encode a number of empty rows
        bytes.addAll(encodeEmptyRows(emptyRowCount));

        emptyRowCount = 0;
      }

      // we have to encode a single row
      Vector<Byte> encodedRow = new Vector<>();
      curY = 0;
      while (curY < 1024) {
        boolean encodingTiles = rgn[curRow][curY];

        int count = 0;

        for (; curY < 1024; ++count, ++curY) {
          if (rgn[curRow][curY] != encodingTiles) break;
        }

        // encode count tiles of type encodingTiles
        encodedRow.addAll(encodeRun(count, encodingTiles));
      }

      boolean sameAsLastRow = true;

      if (lastRow == null) {
        sameAsLastRow = false;
      } else if (lastRow.size() != encodedRow.size()) {
        sameAsLastRow = false;
      } else {
        for (int v = 0; v < lastRow.size(); ++v) {
          if (!lastRow.get(v).equals(encodedRow.get(v))) {
            sameAsLastRow = false;
            break;
          }
        }
      }

      if (sameAsLastRow) {
        lastRowSameCount++;

        if (curRow == 1023) {
          // save same as Last Row #
          bytes.addAll(encodeRepeatLastRow(lastRowSameCount));
        }
      } else {
        if (lastRowSameCount != 0) {
          // save same as Last Row #
          bytes.addAll(encodeRepeatLastRow(lastRowSameCount));

          lastRowSameCount = 0;
        }

        bytes.addAll(encodedRow);
        lastRow = encodedRow;
      }
    }

    return bytes;
  }

  /**
   * Encode a number of empty rows
   *
   * @return a Vector of bytes containing the encoding
   */
  private static Vector<Byte> encodeEmptyRows(int count) {

    /*
     * first, rows that contain no tiles at all can (optionally) be encoded
     * specially:
     *
     * 100n nnnn - n+1 (1-32) rows of all empty 1010 00nn nnnn nnnn - n+1
     * (1-1024) rows of all empty
     */

    Vector<Byte> code = new Vector<>();

    if (count <= 32) {

      count--; // Because it's 1-32, not 0-31.
      byte encode = (byte) (count | 0x80);
      code.add(encode);

    } else {
      count--;
      byte one = (byte) ((count >> 8) | 0xA0);
      byte two = (byte) ((count & 0x00FF));

      code.add(one);
      code.add(two);
    }

    return code;
  }

  /**
   * Encode a run
   *
   * @param count the number of tiles to cover
   * @param inRegion is this a run of region tiles? (or empty spaces)
   * @return the Vector of encodedBytes for this run
   */
  private static Vector<Byte> encodeRun(int count, boolean inRegion) {

    /*
     * for each row, split it into runs of empty tiles and present tiles.
     * for each run, output one of these bit sequences:
     *
     * 000n nnnn - n+1 (1-32) empty tiles in a row 0010 00nn nnnn nnnn - n+1
     * (1-1024) empty tiles in a row 010n nnnn - n+1 (1-32) present tiles in
     * a row 0110 00nn nnnn nnnn - n+1 (1-1024) present tiles in a row
     */

    Vector<Byte> code = new Vector<>();

    if (count <= 32) {
      count--;

      byte one;

      if (!inRegion) {
        one = (byte) count;
      } else {
        one = (byte) (count | 0x40);
      }

      code.add(one);

    } else {

      count--;

      byte one;
      byte two;

      // empty tiles
      if (!inRegion) {
        one = (byte) ((count >> 8) | 0x20);
        two = (byte) (count & 0x00FF);
      }
      // present tiles
      else {
        one = (byte) ((count >> 8) | 0x60);
        two = (byte) (count & 0x00FF);
      }

      code.add(one);
      code.add(two);
    }

    return code;
  }

  /**
   * encode a repeat of the last row
   *
   * @param count the number of times we repeated
   * @return a Vector of Bytes containing the encoding of this repetition
   */
  private static Vector<Byte> encodeRepeatLastRow(int count) {
    /*
     * if the same pattern of tiles appears in more than one consecutive
     * row, you can use these special codes to save more space:
     *
     * 110n nnnn - repeat last row n+1 (1-32) times 1110 00nn nnnn nnnn -
     * repeat last row n+1 (1-1024) times
     */

    Vector<Byte> code = new Vector<>();

    if (count <= 32) {

      count--; // Because it's 1-32, not 0-31.
      byte encode = (byte) (count | 0xC0);
      code.add(encode);

    } else {

      count--;

      byte one = (byte) ((count >> 8) | 0xE0);
      byte two = (byte) ((count & 0x00FF));

      code.add(one);
      code.add(two);
    }

    return code;
  }

  //  /**
  //   * Get this byte in binary
  //   *
  //   * @param b the byte to convert
  //   */
  //  /*
  //   * private static String getBinaryStringOfByte(byte b) { int mask =
  //   * 0x00000080; String rv = "";
  //   *
  //   * for (int x = 0; x < 8; ++x) { rv += ((b & mask) == 0 ? "0" : "1"); mask =
  //   * mask >> 1; }
  //   *
  //   * return rv; }
  //   */

  /**
   * Load the data in this ByteArray into this region
   *
   * @param encoding an eLVL REGN chunk, without the header
   * @return the error String, or null
   */
  public String decodeRegion(ByteArray encoding) {
    String error = null;
    int superChunkLen = encoding.m_array.length;
    int cur = 0;

    while (cur < superChunkLen) {
      if (superChunkLen - cur < 8) // not enough room for subchunk
      // header
      {
        error = "Not enogh bytes to make a subchunk header in REGN superchunk.";
        break;
      }
      String type = encoding.readString(cur, 4);
      cur += 4;
      int len = encoding.readLittleEndianInt(cur);
      cur += 4;

      // "rBSE" - whether the region represents a base in a flag game
      if (type.equals("rBSE") && len == 0) {
        isBase = true;
      }
      // "rNAW" - no antiwarp
      else if (type.equals("rNAW") && len == 0) {
        isNoAnti = true;
      }
      // "rNWP" - no weapons
      else if (type.equals("rNWP") && len == 0) {
        isNoWeps = true;
      }
      // "rNFL" - no flag drops
      else if (type.equals("rNFL") && len == 0) {
        isNoFlags = true;
      }
      // "rAWP" - auto-warp
      else if (type.equals("rAWP")) {
        isAutoWarp = true;
        x = encoding.readLittleEndianShort(cur);
        if (x > 1023) x = -1;

        cur += 2;
        y = encoding.readLittleEndianShort(cur);

        if (y > 1023) y = -1;

        cur += 2;
        if (len == 20) // we also have an arena
        {
          arena = encoding.readNullTerminatedString(cur);
          cur += 16;
        }
      }
      // "rNAM" - a descriptive name for the region
      else if (type.equals("rNAM")) {
        name = encoding.readString(cur, len);
        cur += len;

        int padding = 4 - len % 4;
        if (padding != 4) {
          cur += padding;
        }
      }
      // "rTIL" - tile data, the definition of the region
      else if (type.equals("rTIL")) {
        error = decodeTiles(encoding.m_array, cur, len);

        if (error != null) break;

        cur += len;

        int padding = 4 - len % 4;
        if (padding != 4) {
          cur += padding;
        }
      }
      // other - unknown tiles, or maybe that python stuff
      else {
        // encode header
        unknownBytes.add((byte) type.charAt(0));
        unknownBytes.add((byte) type.charAt(1));
        unknownBytes.add((byte) type.charAt(2));
        unknownBytes.add((byte) type.charAt(3));
        byte[] dword = BitmapSaving.toDWORD(len);
        for (int c = 0; c < 4; ++c) unknownBytes.add(dword[c]);

        // encode data
        int endIndex = cur + len;
        for (int c = cur; c < endIndex; ++c) {
          byte b = encoding.readByte(c);
          unknownBytes.add(b);
        }
        cur += len;

        // encode padding
        int padding = 4 - len % 4;
        if (padding != 4) {
          unknownBytes.add((byte) 0);
          cur += padding;
        }
      }
    }

    if (error == null && cur != superChunkLen) {
      error =
          "REGN chunk eLVL data went past encoded length, cur = "
              + cur
              + ", superChunkLength = "
              + superChunkLen;
    }

    return error;
  }

  /**
   * decode the rTIL section into the vector of rectangles
   *
   * @param data the byte[] of data loaded
   * @param offset the offset to start reading
   * @param size the length to read
   * @return the error String
   */
  private String decodeTiles(byte[] data, int offset, int size) {

    boolean[][] rgn = new boolean[1024][1024];
    String error = null;
    int endByte = offset + size;

    // The region starts empty.
    for (int x = 0; x < 1024; x++) {
      for (int y = 0; y < 1024; y++) {
        rgn[x][y] = false;
      }
    }

    int curX = 0;
    int curY = 0;

    while (offset < endByte) {
      byte typeByte = data[offset];

      int type = getEncodedType(typeByte);
      int len = getEncodedLength(data, offset, type);

      if (type == SMALL_EMPTY_RUN || type == LONG_EMPTY_RUN) {

        if (len + curX > 1024) {
          error = "empty run extends past end";
          break;
        }

        curX += len;

      } else if (type == SMALL_PRESENT_RUN || type == LONG_PRESENT_RUN) {

        if (len + curX > 1024) {
          error = "present run extends past end";
          break;
        }

        int stopX = curX + len;

        for (int x = curX; x < stopX; x++) {
          rgn[curY][x] = true;
        }

        curX += len;
      } else if (type == SMALL_EMPTY_ROWS || type == LONG_EMPTY_ROWS) {
        if (curX != 0) {
          error = "empty row occured before a run was over, curX = " + curX;
          break;
        }

        curY += len;

      } else if (type == SMALL_REPEAT || type == LONG_REPEAT) {

        if (curX != 0) {
          error = "repeat occured before a run was over.";
          break;
        }
        if (curY == 0) {
          error = "repeat occured in the first row.";
          break;
        }

        int stopY = curY + len;
        int copyY = curY - 1;

        for (int x = 0; x < 1024; x++) {
          for (int y = curY; y < stopY; y++) {
            rgn[y][x] = rgn[copyY][x];
          }
        }

        curY += len;
      }

      if (curX == 1024) {
        curY++;
        curX = 0;
      }

      if (type % 2 == 0) {
        // Short.
        offset++;
      } else {
        // Long.
        offset += 2;
      }
    }

    if (error == null && curY != 1024) {
      error = "Encoded rTIL does NOT contain 1024 rows... it has " + curY;
    }

    if (error == null) {
      // at this point we're all encoded, make the rectangles
      curX = curY = 0;

      while (curY < 1024) {
        if (rgn[curY][curX]) {

          Rectangle r = new Rectangle();
          r.x = curX;
          r.y = curY;

          int w = 1;
          for (int x = curX + 1; x < 1024; x++) {

            if (!rgn[curY][x]) {
              break;
            }

            w++;
          }

          r.width = w;

          int h = 1;
          for (int y = r.y + 1; y < 1024; y++) {

            int endX = r.x + r.width;
            boolean sameRow = true;

            for (int x = r.x; x < endX; x++) {

              if (!rgn[y][x]) {
                sameRow = false;
                break;
              }
            }

            if (!sameRow) {
              break;
            }

            h++;
          }

          r.height = h;

          int endX = r.x + r.width;
          int endY = r.y + r.height;

          for (int y = r.y; y < endY; y++)
            for (int x = r.x; x < endX; x++) {

              // This part has been processed.
              rgn[y][x] = false;
            }

          rects.add(r);
        }

        // Increment.
        if (++curX == 1024) {

          curX = 0;
          curY++;
        }
      }
    }

    return error;
  }

  /**
   * Get a random Color
   *
   * @return a random Color
   */
  private static Color getRandomColor() {
    int r = (int) (Math.random() * 255);
    int g = (int) (Math.random() * 255);
    int b = (int) (Math.random() * 255);

    if (r + b + g < 40) {
      r += 20;
      g += 20;
      b += 20;
    }

    return new Color(r, g, b);
  }

  /**
   * Get an encoded length for this rTil entry
   *
   * @param data the data array
   * @param offset the current offset
   * @param type the type of the fragment at offset in data
   * @return the length that's encoded
   */
  private static int getEncodedLength(byte[] data, int offset, int type) {
    if (type % 2 == 0) {
      // Short.
      return getEncodedLength(data[offset]);
    } else {
      return getEncodedLength(data[offset], data[offset + 1]);
    }
  }

  /**
   * Get the encoded length in this SMALL one byte rTIL encoding
   *
   * @param one the byte for this encoding
   * @return an in 1-32 represeting the length that's currently encoded
   */
  private static int getEncodedLength(byte one) {
    return getBitFragment(one, 4, 8) + 1;
  }

  /**
   * Get the encoded length in this LONG two byte rTIL encoding
   *
   * @param one the first byte for this encoding
   * @param two the second byte for this encoding
   * @return an in 1-1024 represeting the length that's currently encoded
   */
  private static int getEncodedLength(byte one, byte two) {
    int highByte = getBitFragment(one, 7, 8) << 8;
    return (highByte | (0xFF & two)) + 1;
  }

  /**
   * Get the type of encoding this is for eLVL rTIL encoding... will be a constant such as
   * Region.SMALL_EMPTY_RUN or Region.LONG_REPEAT
   */
  private static int getEncodedType(byte typeByte) {
    return getBitFragment(typeByte, 1, 3);
  }

  /**
   * get the bit fragment from startIndex to endIndex
   *
   * @param extractFrom the byte to extract from
   * @param startIndex the inclusive leftbound index: 1234 5678
   * @param endIndex the inclusive rightbound index 1234 5678 and > startIndex
   * @return the int extracted from the requested bits
   */
  private static int getBitFragment(byte extractFrom, int startIndex, int endIndex) {
    int shift = 8 - endIndex;
    int numBits = endIndex - startIndex + 1;
    byte mask = (byte) ((0x01 << numBits) - 1);

    return (extractFrom >> shift) & mask;
  }
}
