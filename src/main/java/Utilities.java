import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
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
	public static void requestPetData() {
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
	 * Connects to the battlenet API and makes requests for data
	 * for each pet ability listed in the pet ability index api
	 * call.
	 * 
	 * This method makes ~700 requests at the time of writing, and
	 * takes a few minutes to complete.
	 */
	public static void requestAbilityData() {
		try {
			BattlenetAPIConnection api = new BattlenetAPIConnection();
			
			if(!api.connect()) {
				System.out.println("Unable to retrieve token for bnet api.");
				return;
			}
			
			// Get index of abilities.
			ArrayList<NameValuePair> params = new ArrayList<>();
			params.add(new BasicNameValuePair("namespace", "static-us"));
			params.add(new BasicNameValuePair("locale", "en_US"));
			JsonObject obj = api.request("https://us.api.blizzard.com/data/wow/pet-ability/index", params);
			params.clear();
			
			JsonArray abilities = obj.get("abilities").getAsJsonArray();
			
			JsonArray data = new JsonArray();
			
			int processed = 1;
			
			// Look at each ability, grab it's info, add it to the database
			for(JsonElement abilityElement : abilities) {
				String abilityId = abilityElement.getAsJsonObject().get("id").getAsString();
				
				params.add(new BasicNameValuePair("namespace", "static-us"));
				params.add(new BasicNameValuePair("locale", "en_US"));
				obj = api.request("https://us.api.blizzard.com/data/wow/pet-ability/" + abilityId, params);
				params.clear();
				
				int id = obj.get("id").getAsInt();
				String name = obj.get("name").getAsString();
				String type = obj.get("battle_pet_type").getAsJsonObject().get("type").getAsString();
				
				JsonObject abilityObj = new JsonObject();
				abilityObj.addProperty("id", id);
				abilityObj.addProperty("name", name);
				abilityObj.addProperty("type", type);
				data.add(abilityObj);
				
				System.out.printf("Added ability %s (%d/%d)\n", name, processed++, abilities.size());
				
			}
			
			JsonWriter writer = new JsonWriter(new FileWriter("ability_data.json"));
			Json.toJson(data, writer);
			writer.flush();
			writer.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Converts the pet_data.json file into a database file.
	 */
	public static void createPetDatabaseFromJson() {
		Connection conn = null;
		try {	
			// Load driver and make connection
			Class.forName("org.h2.Driver");
			conn = DriverManager.getConnection("jdbc:h2:./data", "user", "");
			
			// Drop existing data
			Statement stmt = conn.createStatement();
			String sql = "DROP TABLE IF EXISTS pet_data";
			stmt.execute(sql);
			
			sql = "DROP TABLE IF EXISTS pet_abilities";
			stmt.execute(sql);
			
			// Create pet data table
			stmt = conn.createStatement();
			sql = "CREATE TABLE pet_data "
					+ "("
					+ " id INT NOT NULL,"
					+ " name VARCHAR(255) NOT NULL,"
					+ " type VARCHAR(255) NOT NULL,"
					+ " is_capturable BOOL NOT NULL,"
					+ " is_battlepet BOOL NOT NULL,"
					+ " is_alliance_only BOOL NOT NULL,"
					+ " is_horde_only BOOL NOT NULL,"
					+ " source VARCHAR(255) NOT NULL,"
					+ " aquatic_ability BOOL NOT NULL DEFAULT 0,"
					+ " beast_ability BOOL NOT NULL DEFAULT 0,"
					+ " critter_ability BOOL NOT NULL DEFAULT 0,"
					+ " dragonkin_ability BOOL NOT NULL DEFAULT 0,"
					+ " elemental_ability BOOL NOT NULL DEFAULT 0,"
					+ " flying_ability BOOL NOT NULL DEFAULT 0,"
					+ " humanoid_ability BOOL NOT NULL DEFAULT 0,"
					+ " magic_ability BOOL NOT NULL DEFAULT 0,"
					+ " mechanical_ability BOOL NOT NULL DEFAULT 0,"
					+ " undead_ability BOOL NOT NULL DEFAULT 0,"
					+ " PRIMARY KEY ( id )"
					+ ")";
			stmt.execute(sql);
			
			// Create pet abilities table
			sql = "CREATE TABLE pet_abilities"
					+ "("
					+ " pet_id INT NOT NULL,"
					+ " ability_id INT NOT NULL,"
					+ " CONSTRAINT pk PRIMARY KEY (pet_id, ability_id)"
					+ ")";
			stmt.execute(sql);
			
			// Load json pet data
			JsonReader reader = new JsonReader(new FileReader("pet_data.json"));
			Type type = new TypeToken<ArrayList<Pet>>() {}.getType();
			ArrayList<Pet> pets = Json.fromJson(reader, type);
			
			for(Pet pet : pets) {
				// Ignore non-tradable pets
				if(!pet.isTradable)
					continue;
				
				// Insert pet data
				sql = String.format("INSERT INTO pet_data (id, name, type, is_capturable, is_battlepet, is_alliance_only, is_horde_only, source) "
						+ "VALUES (%d, '%s', '%s', %d, %d, %d, %d, '%s')", 
						pet.id,
						pet.name.replace("'", "''"),
						pet.type.replace("'", "''"),
						pet.isCapturable ? 1 : 0,
						pet.isBattlepet ? 1 : 0,
						pet.isAllianceOnly ? 1 : 0,
						pet.isHordeOnly ? 1 : 0,
						pet.source.replace("'", "''")
				);
				System.out.println(sql);
				stmt.execute(sql);
				
				// Add entries for pet abilities
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
	 * Creates the ability_data table using the ability_data.json
	 * file, and updates the pet_data table to include columns for
	 * each ability id.
	 */
	public static void addPetAbilityDataToDatabase() {
		Connection conn = null;
		try {
			// Connect to database.
			Class.forName("org.h2.Driver");
			conn = DriverManager.getConnection("jdbc:h2:./data", "user", "");

			// Drop existing data
			Statement stmt = conn.createStatement();
			String sql = "DROP TABLE IF EXISTS ability_data";
			stmt.execute(sql);

			// Create new table
			sql = "CREATE TABLE ability_data "
					+ "("
					+ "id INT NOT NULL,"
					+ "name VARCHAR(255) NOT NULL,"
					+ "type VARCHAR(255) NOT NULL,"
					+ "PRIMARY KEY ( id )"
					+ ")";
			stmt.execute(sql);
			
			// Read ability data from the file
			JsonReader reader = new JsonReader(new FileReader("ability_data.json"));
			JsonArray abilities = Json.fromJson(reader, JsonArray.class);
			reader.close();
			
			for(JsonElement abilityElement : abilities) {
				JsonObject ability = abilityElement.getAsJsonObject();
				
				int id = ability.get("id").getAsInt();
				String name = ability.get("name").getAsString();
				String type = ability.get("type").getAsString();
				
				sql = String.format("INSERT INTO ability_data VALUES (%d, '%s', '%s')",
						id,
						name.replace("'", "''"),
						type.replace("'", "''")
				);
				stmt.execute(sql);
				System.out.println(sql);

				// Also add this ability id as a column to the pet_data table
				sql = "ALTER TABLE pet_data ADD ability_" + id + " BOOL NOT NULL DEFAULT 0";
				stmt.execute(sql);
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			if(conn != null) {
				try {
					conn.close();
				} catch(SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Updates the pet_data table in the database to set the
	 * values for which abilities this pet possesses.
	 */
	public static void updatePetDataWithAbilities() {
		Connection conn = null;
		try {
			// Load driver and make connection
			Class.forName("org.h2.Driver");
			conn = DriverManager.getConnection("jdbc:h2:./data", "user", "");
			
			// Look through each existing pet
			Statement stmt = conn.createStatement();
			String sql = "SELECT id FROM pet_data";
			
			ResultSet pets = stmt.executeQuery(sql);
			
			while(pets.next()) {
				int petId = pets.getInt("id");
				
				sql = "SELECT ability_id FROM pet_abilities WHERE pet_id = " + petId;
				ResultSet abilities = conn.createStatement().executeQuery(sql);
				
				// Update ability columns for the pet
				while(abilities.next()) {
					int abilityId = abilities.getInt("ability_id");
					
					sql = "UPDATE pet_data SET ability_" + abilityId + " = 1 WHERE id = " + petId;
					conn.createStatement().executeUpdate(sql);
					
					System.out.println(sql);
					
					// Update ability types
					sql = "SELECT type FROM ability_data WHERE id = " + abilityId;
					ResultSet ability = conn.createStatement().executeQuery(sql);
					
					while(ability.next()) {
						String type = ability.getString("type").toLowerCase();
						sql = "UPDATE pet_data SET " + type + "_ability = 1 WHERE id = " + petId;
						conn.createStatement().executeUpdate(sql);
					}
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
	 * Cleans an auction data json dump file, leaving
	 * only the auction entries which are for battle
	 * pets.
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
	
	/**
	 * Builds the auction database using the cleaned auction
	 * json files found in the present directory.
	 */
	public static void addAuctionDataToDatabase() {
		// Load driver class
		try {
			Class.forName("org.h2.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		Connection conn = null;
		try {
			conn = DriverManager.getConnection("jdbc:h2:./data", "user", "");
			
			Statement stmt = conn.createStatement();
			
			// Delete existing data
			String sql = "DROP TABLE IF EXISTS auction_data";
			stmt.execute(sql);
			
			// Create the table
			sql = "CREATE TABLE auction_data ("
					+ "id INT NOT NULL,"
					+ "pet_id INT NOT NULL,"
					+ "buyout BIGINT NOT NULL,"
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
									auctionObj.get("buyout").getAsLong(),
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
	
	/**
	 * Uses the auction data in the database to calculate each
	 * pet's value and adds that value to the pet_data table.
	 */
	public static void calculatePetValuesAndAddToDatabase() {
		Connection conn = null;
		try {
			// Load driver and make connection
			Class.forName("org.h2.Driver");
			conn = DriverManager.getConnection("jdbc:h2:./data", "user", "");
			
			// Add new column
			String sql = "ALTER TABLE pet_data ADD (value REAL, value_winsor REAL)";
			conn.createStatement().execute(sql);
			
			// Look through each pet
			sql = "SELECT * FROM pet_data";
			ResultSet pets = conn.createStatement().executeQuery(sql);
			
			while(pets.next()) {
				int petId = pets.getInt("id");
				
				sql = "SELECT * FROM auction_data WHERE pet_id = " + petId + " ORDER BY realm_id";
				ResultSet auctions = conn.createStatement().executeQuery(sql);
				
				ArrayList<Double> values = new ArrayList<>();
				while(auctions.next()) {
					double buyout = auctions.getDouble("buyout");					
					values.add(buyout / 10000f);
				}
				
				// Calculate the average value
				float total = 0;
				for(double f : values) {
					total += f;
				}
				float avg = total / values.size();
				
				float std = 0;
				for(double f : values) {
					std += Math.pow(Math.abs(f - avg), 2);
				}
				std /= values.size();
				std = (float) Math.sqrt(std);
				
				// Calculate the average value using a modified winsor mean method
				Collections.sort(values);
				float winpercent = 0.2f;
				int edge = (int) (values.size() * winpercent);
				for(int i = values.size() - 1; i > values.size() - edge; i--) {
					values.set(i, values.get(values.size() - edge));
				}
				
				float wintotal = 0;
				for(double f : values) {
					wintotal += f;
				}
				float winavg = wintotal / values.size();
				
				float winstd = 0;
				for(double f : values) {
					winstd += Math.pow(Math.abs(f - winavg), 2);
				}
				winstd /= values.size();
				winstd = (float) Math.sqrt(winstd);
				
				// Print values
				System.out.printf("Pet %s:\n\tAvg:\t%.2f\n\tStd:\t%.2f\n\tAvg_w:\t%.2f\n\tStd_w:\t%.2f\n", petId, avg, std, winavg, winstd);
				
				// Update database into database
				sql = "UPDATE pet_data SET value = " + avg + ", value_winsor = " + winavg + " WHERE id = " + petId;
				conn.createStatement().execute(sql);
			}
			
		} catch (Exception e) {
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
