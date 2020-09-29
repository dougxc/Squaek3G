/*
 * HexEncoding.java
 *
 * Created on November 15, 2005, 1:13 PM
 */

package com.sun.squawk.security;

/**
 * @author msbrimne
 */
// Encode / Decode hexadecimal strings to / from byte arrays
public class HexEncoding 
{
    /** Creates a new instance of HexEncoding */
    public HexEncoding() 
    {
    }
    
    /** Hexadecimal character digits (0-f). */
    private static char[] hc = 
    {
	'0', '1', '2', '3', '4', '5', '6', '7', 
	'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };
    
    public static String hexEncode(byte[] b)
    {
        return HexEncoding.hexEncode(b,b.length);
    }
    
    public static String hexEncode(byte[] b, int len) 
    {
	if (b == null)
        {
	    return ("");
        }
	else 
        {
	    char[] r = new char[len << 1];
	    int v;
	    for (int i = 0, j = 0; i < len; i++) 
            {
		v = b[i] & 0xff;
		r[j++] = hc[v >>> 4];
		r[j++] = hc[v & 0x0f];
	    }
	    return (new String(r));
	}
    }     
    
    public static byte[] hexDecode(String str) 
    {
        int len = (str.length() + 1) / 2;
        byte[] res = new byte[len];
        hexDecode(str, res, 0, len);
        return res;
    }
    
    /**
     * Decodes the given hex-string to a byte-array. Returns number of bytes
     * actually used.
     */
    private static int hexDecode(String str, byte[] b, int ofs, int len) 
    {
        int strLen = str.length();
        if ((strLen % 2) == 1) 
        {
            str = "0" + str;
            strLen++;
        }
        if ((len * 2) > strLen) 
        {
            len = strLen / 2;
        }
        for (int i = 0; i < strLen; i += 2) 
        {
            b[ofs++] = (byte)((fromHex(str.charAt(i)) << 4) + fromHex(str.charAt(i + 1)));
        }
        return len;
    }
    
    private static int fromHex(char ch) 
    {
        if ((ch >= '0') && (ch <= '9')) 
        {
            return (int)(ch - '0');
        } 
        else if ((ch >= 'a') && (ch <= 'f')) 
        {
            return (int)(ch - 'a') + 10;
        } 
        else if ((ch >= 'A') && (ch <= 'F')) 
        {
            return (int)(ch - 'A') + 10;
        } 
        else 
        {
            return 0;
        }
    }   
}
