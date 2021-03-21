import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

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
	
}
