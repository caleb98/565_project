import java.io.File;
import java.util.Scanner;

public class Start {
	
	public static void main(String[] args) {
		
		Scanner keyboard = new Scanner(System.in);
		
		System.out.println("Utilities for Caleb's CS 565 Project");
		System.out.println("------------------------------------");
		
		int selection = -1;
		while(selection != 0) {
			printMenu();
			selection = getSelection(keyboard.nextLine());
			switch(selection) {
			
			case 0:
				System.out.println("Exiting.");
				break;
				
			case 1:
				if(hasEnvironmentVariables()) {
					Utilities.requestPetData();
				}
				break;
				
			case 2:
				if(hasEnvironmentVariables()) {
					Utilities.requestAbilityData();
				}

				break;
				
			case 3:
				if(!(new File("pet_data.json")).exists()) {
					System.out.println("pet_data.json file does not exist. Cannot create database.");
					break;
				}
				Utilities.createPetDatabaseFromJson();
				System.out.println();
				break;
				
			case 4:
				Utilities.addPetAbilityDataToDatabase();
				break;
				
			case 5:
				Utilities.updatePetDataWithAbilities();
				break;
				
			case 6:
				if(hasEnvironmentVariables()) {
					Utilities.dumpAuctionData();
				}
				break;
				
			case 7:
				Utilities.cleanAuctionData();
				break;
				
			case 8:
				Utilities.addAuctionDataToDatabase();
				break;
			
			case 9:
				Utilities.calculatePetValuesAndAddToDatabase();
				break;
				
			}
		}
		
		keyboard.close();
		
	}
	
	private static boolean hasEnvironmentVariables() {
		if(System.getenv("BNET_CLIENT_ID") == null) {
			System.out.println("BNET_CLIENT_ID environment variable not set!");
			return false;
		}
		else if(System.getenv("BNET_CLIENT_SECRET") == null) {
			System.out.println("BNET_CLIENT_SECRET environment variable not set!");
			return false;
		}
		return true;
	}
	
	private static int getSelection(String input) {
		try {
			return Integer.parseInt(input);
		} catch (NumberFormatException e) {
			System.out.println("Invalid input.");
			return -1;
		}
	}
	
	private static void printMenu() {
		System.out.println("Select an action:");
		System.out.println("0. Exit");
		System.out.println("1. Request Pet Data*");
		System.out.println("2. Request Ability Data*");
		System.out.println("3. Build Pet Database from Json File");
		System.out.println("4. Add Ability Data to Database");
		System.out.println("5. Update Pet Data with Abilities");
		System.out.println("6. Dump Auction Data*");
		System.out.println("7. Clean Auction Data");
		System.out.println("8. Add Auction Data to Database");
		System.out.println("9. Calculate Pet Values and Add to Database");
		System.out.println("(*Requires BNET_CLIENT_ID and BNET_CLIENT_SECRET environment variables.)");
	}
	
}
