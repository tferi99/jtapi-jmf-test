package org.ftoth.jtapijmftest;

public class TestInheritance
{
	public static void main(String[] args)
	{
		Parent p = new Parent();
		Child c = new Child();

		p.func();
		c.func();

		Parent p2 = p;
		Funcable p3 = c;

		p2.func();
		p3.func();
		//p3.generate();
	}
}

abstract class GrandParent
{
	public void generate()
	{
		String result = calculate();
		System.out.println("GrandParent.generate() --> " + result);
	}

	abstract protected String calculate();
}

class Parent extends GrandParent implements Funcable
{
	/*
	 * public void valami() { func(); }
	 */

	public void func()
	{
		System.out.println("Parent.func()");
	}

	@Override
	protected String calculate()
	{
		return "Calculated by Parent";
	}
}


class Parent2 implements Func2able
{
	@Override
	public void func2()
	{
		System.out.println("Parent2.func2()");
	}
}

class Child extends Parent implements Funcable, Func2able
{
	/*
	 * public void mas() { func(); }
	 */

	@Override
	public void func()
	{
		System.out.println("Child.func()");
	}

	@Override
	public void func2()
	{
		System.out.println("Child.func2()");
	}

}

interface Funcable
{
	void func();
}

interface Func2able
{
	void func2();
}

