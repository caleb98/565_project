import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class Utilities {

	/**
	 * Connects to the battlenet API and makes requests for data
	 * for each pet listed in the pet index api call.
	 * 
	 * This method will take a long time and makes 1k+ http requests.
	 */
	public static void requestPetDataFromBnetApi() {
		try {
			BattlenetAPIConnection api = new BattlenetAPIConnection();
			
			if(!api.connect()) {
				System.out.println("Unable to retrieve token for bnet api.");
				return;
			}
			
			ArrayList<Pet> allPets = new ArrayList<>();
			
			//Gets pet ids and names
			ArrayList<NameValuePair> params = new ArrayList<>();
			params.add(new BasicNameValuePair("namespace", "static-us"));
			params.add(new BasicNameValuePair("locale", "en_US"));
			JsonObject obj = api.request("https://us.api.blizzard.com/data/wow/pet/index", params);
			
			JsonArray pets = obj.get("pets").getAsJsonArray();

			//Loop through each pet id and grab detailed info
			int num = 1;
			for(JsonElement petEntry : pets) {
				
				//Get current pet id and request url
				JsonObject pet = petEntry.getAsJsonObject();
				int petId = pet.get("id").getAsInt();
				String requestUrl = String.format("https://us.api.blizzard.com/data/wow/pet/%d", petId);
				
				//Make request
				params.clear();
				params.add(new BasicNameValuePair("namespace", "static-us"));
				params.add(new BasicNameValuePair("locale", "en_US"));
				JsonObject petData = api.request(requestUrl, params);
				
				//Get all pet data from json
				int id = petData.get("id").getAsInt();
				String name = petData.get("name").getAsString();
				String type = petData.get("battle_pet_type").getAsJsonObject().get("type").getAsString();
				boolean capturable = petData.get("is_capturable").getAsBoolean();
				boolean tradable = petData.get("is_tradable").getAsBoolean();
				boolean battlepet = petData.get("is_battlepet").getAsBoolean();
				boolean allianceOnly = petData.get("is_alliance_only").getAsBoolean();
				boolean hordeOnly = petData.get("is_horde_only").getAsBoolean();
				
				//Some pets don't have abilities, so they're obviously not battle pets. Just ignore these
				if(!petData.has("abilities")) {
					continue;
				}
				
				JsonArray abilities = petData.get("abilities").getAsJsonArray();
				int[] abilityIds = new int[abilities.size()];
				for(int i = 0; i < abilities.size(); i++) {
					abilityIds[i] = abilities.get(i).getAsJsonObject().get("ability").getAsJsonObject().get("id").getAsInt();
				}
				
				//Some pets don't have a source field, so if it doesn't exist specify that one 
				//wasn't provided.
				String source;
				if(petData.has("source")) {
					source = petData.get("source").getAsJsonObject().get("type").getAsString();
				}
				else {
					source = "NONE_PROVIDED";
				}
				
				//Create pet object and add to list
				allPets.add(new Pet(id, name, type, capturable, tradable, battlepet, allianceOnly, hordeOnly, abilityIds, source));
				
				System.out.printf("Added pet %s (%d)\t%d/%d\n", name, id, num++, pets.size());
			}
			
			//All pet data grabbed, save json to file
			JsonWriter writer = new JsonWriter(new FileWriter("pet_data.json"));
			Type petCollectionType = new TypeToken<ArrayList<Pet>>() {}.getType();
			Json.toJson(allPets, petCollectionType, writer);
			writer.flush();
			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Converts the pet_data.json file into a database file.
	 */
	public static void createPetDatabaseFromJson() {
		Connection conn = null;
		try {	
			//See if database exists. If so, delete
			File db = new File("./pet_data.mv.db");
			if(db.exists()) {
				db.delete();
			}
			
			//Load driver and make connection
			Class.forName("org.h2.Driver");
			conn = DriverManager.getConnection("jdbc:h2:./pet_data", "user", "");
			
			//Create pet data table
			Statement stmt = conn.createStatement();
			String sql = "CREATE TABLE pet_data "
					+ "("
					+ " id INT NOT NULL,"
					+ " name VARCHAR(255) NOT NULL,"
					+ " type VARCHAR(255) NOT NULL,"
					+ " is_capturable BOOL NOT NULL,"
					+ " is_tradable BOOL NOT NULL,"
					+ " is_battlepet BOOL NOT NULL,"
					+ " is_alliance_only BOOL NOT NULL,"
					+ " is_horde_only BOOL NOT NULL,"
					+ " source VARCHAR(255) NOT NULL,"
					+ " PRIMARY KEY ( id )"
					+ ")";
			stmt.execute(sql);
			
			//Create pet abilities table
			sql = "CREATE TABLE pet_abilities"
					+ "("
					+ " pet_id INT NOT NULL,"
					+ " ability_id INT NOT NULL,"
					+ " CONSTRAINT pk PRIMARY KEY (pet_id, ability_id),"
					+ " FOREIGN KEY (pet_id) REFERENCES pet_data(id)"
					+ ")";
			stmt.execute(sql);
			
			//Load json pet data
			JsonReader reader = new JsonReader(new FileReader("pet_data.json"));
			Type type = new TypeToken<ArrayList<Pet>>() {}.getType();
			ArrayList<Pet> pets = Json.fromJson(reader, type);
			
			for(Pet pet : pets) {
				//Insert pet data
				sql = String.format("INSERT INTO pet_data VALUES (%d, '%s', '%s', %d, %d, %d, %d, %d, '%s')", 
						pet.id,
						pet.name.replace("'", "''"),
						pet.type.replace("'", "''"),
						pet.isCapturable ? 1 : 0,
						pet.isTradable ? 1 : 0,
						pet.isBattlepet ? 1 : 0,
						pet.isAllianceOnly ? 1 : 0,
						pet.isHordeOnly ? 1 : 0,
						pet.source.replace("'", "''")
				);
				System.out.println(sql);
				stmt.execute(sql);
				
				//Add entries for pet abilities
				int[] abilities = pet.abilities;
				for(int ability : abilities) {
					sql = String.format("INSERT INTO pet_abilities VALUES (%d, %d)", 
							pet.id,
							ability
					);
					System.out.println("\t" + sql);
					stmt.execute(sql);
				}
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		} finally {	
			if(conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Dumps auction data from all realms into a single JSON file.
	 */
	public static void dumpAuctionData() {
		try {
			BattlenetAPIConnection conn = new BattlenetAPIConnection();
			
			if(!conn.connect()) {
				System.out.println("Unable to retrieve token for BattleNet api.");
				return;
			}
			
			// Get a list of realms
			ArrayList<NameValuePair> params = new ArrayList<>();
			params.add(new BasicNameValuePair("namespace", "dynamic-us"));
			params.add(new BasicNameValuePair("locale", "en_US"));
			
			JsonObject realmJson = conn.request("https://us.api.blizzard.com/data/wow/connected-realm/index", params);
			JsonArray realmsArray = realmJson.get("connected_realms").getAsJsonArray();
			
			// Loop through each realm and build auction data
			JsonObject auctionData = new JsonObject();
			auctionData.add("data", new JsonArray());
			int num = 1;
			for(JsonElement realmEntry : realmsArray) {
				
				JsonObject realmAuctionData = new JsonObject();
				JsonObject entry = realmEntry.getAsJsonObject();
				
				// Get request url
				String url = entry.get("href").getAsString();
				
				// Make request
				params.clear();
				JsonObject realmInfo = conn.request(url, params);

				// Get the realm id
				int realmId = realmInfo.get("id").getAsInt();
				
				System.out.printf("Getting auction data for realm group id %d (%d/%d)...",
						realmId, num, realmsArray.size());
				
				// Request auction data for realm id
				url = realmInfo.get("auctions").getAsJsonObject().get("href").getAsString();
				System.out.println("\t" + url);
				params.clear();
				JsonObject auctions = conn.request(url, params);
				
				JsonArray auctionsArray = auctions.get("auctions").getAsJsonArray();
				
				realmAuctionData.addProperty("realmId", realmId);
				realmAuctionData.add("auctions", auctionsArray);
				
				auctionData.get("data").getAsJsonArray().add(realmAuctionData);
				
				num++;
			}
			
			// Save auction data to a file
			LocalDateTime now = LocalDateTime.now();
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd");
			String date = dtf.format(now);
			JsonWriter writer = new JsonWriter(new FileWriter("auction_data_" + date + ".json"));
			Json.toJson(auctionData, writer);
			writer.flush();
			writer.close();
			
			System.out.println("Auction data dumped!");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 */
	public static void cleanAuctionData() {
		
		// Get list of files that are available
		File[] files = new File(".").listFiles((f) -> {
			return f.getName().matches("^auction_data_.*");
		});
		
		// Display list
		System.out.println("\nAvailable Data Files:");
		for(int i = 0; i < files.length; i++) {
			System.out.printf("\t%d. %s\n", i+1, files[i].getName());
		}
		
		// Get selection from user
		@SuppressWarnings("resource")
		Scanner keyboard = new Scanner(System.in);
		int input = -1;
		while(input == -1) {
			System.out.print("\nEnter file # to clean: ");
			try {
				input = Integer.valueOf(keyboard.nextLine());
			} catch (Exception e) {
				System.out.println("Invalid file number.");
				continue;
			}
			
			if(input < 1 || input > files.length) {
				System.out.println("Invalid file number.");
				input = -1;
			}
		}
		
		// Read in the data
		File fileToClean = files[input - 1];
		JsonReader reader;
		try {
			reader = new JsonReader(new FileReader(fileToClean));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		JsonObject dataRaw = Json.fromJson(reader, JsonObject.class);
		JsonArray data = dataRaw.get("data").getAsJsonArray();
		
		// Look through the data.
		for(JsonElement realmData : data) {
			JsonObject realmDataObj = realmData.getAsJsonObject();
			JsonArray auctions = realmDataObj.get("auctions").getAsJsonArray();
			Iterator<JsonElement> iter = auctions.iterator();
			while(iter.hasNext()) {
				// See if pet with id exists
				JsonObject auction = iter.next().getAsJsonObject();
				JsonObject item = auction.get("item").getAsJsonObject();
				int itemId = item.get("id").getAsInt();
				
				// 82800 is the id for "Pet Cage" which all battle pets are sold as
				if(itemId != 82800) {
					iter.remove();
				}
				else {
					System.out.println("Keeping auction for pet id " + item.get("pet_species_id").getAsInt());
				}
			}
		}
		
		// Json now removed all non-pet entries. Save it as a cleaned version.
		try {
			File output = new File("cleaned_" + fileToClean.getName());
			JsonWriter writer = new JsonWriter(new FileWriter(output));
			Json.toJson(dataRaw, writer);
			writer.flush();
			writer.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
			
		// Loading in the data has a lot of overhead, so go ahead and
		// garbage collect once we're done.
		System.gc();
	}
	
	public static void createAuctionDatabase() {
		// Load driver class
		try {
			Class.forName("org.h2.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		// Delete database if exists
		File db = new File("./auction_data.mv.db");
		if(db.exists()) {
			db.delete();
		}
		
		Connection conn = null;
		try {
			conn = DriverManager.getConnection("jdbc:h2:./auction_data", "user", "");
			
			Statement stmt = conn.createStatement();
			
			// Create the table if necessary
			String sql = "CREATE TABLE auction_data ("
					+ "id INT NOT NULL,"
					+ "pet_id INT NOT NULL,"
					+ "buyout INT NOT NULL,"
					+ "realm_id INT NOT NULL,"
					+ "PRIMARY KEY ( id )"
					+ ")";
			stmt.execute(sql);
			
			// Get auction data files
			File[] files = new File(".").listFiles((f) -> {
				return f.getName().startsWith("cleaned_auction_data_");
			});
			
			for(File jsonFile : files) {
				try {
					// Get raw data
					JsonReader reader = new JsonReader(new FileReader(jsonFile));
					JsonObject dataRaw = Json.fromJson(reader, JsonObject.class);
					JsonArray realms = dataRaw.get("data").getAsJsonArray();
					
					// Loop through each realm's data
					for(JsonElement realmRaw : realms) {
						JsonObject realmObj = realmRaw.getAsJsonObject();
						JsonArray auctions = realmObj.get("auctions").getAsJsonArray();
						int realmId = realmObj.get("realmId").getAsInt();
						
						// Loop through each auction on this realm
						for(JsonElement auction : auctions) {
							JsonObject auctionObj = auction.getAsJsonObject();
							
							// If no buyout is listed, ignore.
							if(!auctionObj.has("buyout")) {
								continue;
							}
							
							sql = String.format("INSERT INTO auction_data VALUES (%d, %d, %d, %d)", 
									auctionObj.get("id").getAsInt(),
									auctionObj.get("item").getAsJsonObject().get("pet_species_id").getAsInt(),
									auctionObj.get("buyout").getAsInt(),
									realmId
							);
							
							stmt.execute(sql);
							
							System.out.println(sql);
							
						}
					}
				} catch (IOException e) {
					System.err.println("Unable to process auction data file " + jsonFile.getName() + ": " + e.getMessage());
				}
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if(conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}
