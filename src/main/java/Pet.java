

public class Pet {

	public final int id;
	public final String name;
	public final String type;
	public final boolean isCapturable;
	public final boolean isTradable;
	public final boolean isBattlepet;
	public final boolean isAllianceOnly;
	public final boolean isHordeOnly;
	public final int[] abilities;
	public final String source;
	
	public Pet(int id, String name, String type, boolean isCapturable, boolean isTradable, boolean isBattlepet,
			boolean isAllianceOnly, boolean isHordeOnly, int[] abilities, String source) {
		this.id = id;
		this.name = name;
		this.type = type;
		this.isCapturable = isCapturable;
		this.isTradable = isTradable;
		this.isBattlepet = isBattlepet;
		this.isAllianceOnly = isAllianceOnly;
		this.isHordeOnly = isHordeOnly;
		this.abilities = abilities;
		this.source = source;
	}
	
}
