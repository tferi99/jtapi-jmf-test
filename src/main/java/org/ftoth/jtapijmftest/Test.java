package org.ftoth.jtapijmftest;

import java.io.File;


public class Test
{
	public static void main(String[] args)
	{
		String s = "2012-06-13";
		String x = s.replace('-', File.separatorChar);
		
		System.out.println(x);
	}
}

