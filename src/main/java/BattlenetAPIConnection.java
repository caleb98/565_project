import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import com.google.gson.JsonObject;

public class BattlenetAPIConnection {

	private static final String CLIENT_ID = System.getenv("BNET_CLIENT_ID");
	private static final String CLIENT_SECRET = System.getenv("BNET_CLIENT_SECRET");

	private CloseableHttpClient client;
	private String accessToken;
	
	public BattlenetAPIConnection() {
		client = HttpClients.createDefault();
	}
	
	public boolean connect()
			throws ClientProtocolException, IOException, AuthenticationException {
		
		//Create post request
		HttpPost tokenRequest = new HttpPost("https://us.battle.net/oauth/token");
		
		//Add params
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("grant_type", "client_credentials"));
		tokenRequest.setEntity(new UrlEncodedFormEntity(params));
	
		//Set credentials
		UsernamePasswordCredentials creds = new UsernamePasswordCredentials(CLIENT_ID, CLIENT_SECRET);
		tokenRequest.addHeader(new BasicScheme().authenticate(creds, tokenRequest, null));
		
		//Make request
		CloseableHttpResponse authResponse = client.execute(tokenRequest);
		
		//Check for 200 code
		if(authResponse.getStatusLine().getStatusCode() != 200) {
			return false;
		}
		
		String jsonString = new String(authResponse.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
		JsonObject response = Json.fromJson(jsonString, JsonObject.class);
		
		if(!response.has("access_token")) {
			return false;
		}
		
		accessToken = response.get("access_token").getAsString();
		return true;
	}
	
	public JsonObject request(String target, List<NameValuePair> params)
			throws URISyntaxException, ClientProtocolException, IOException, NoTokenException {
		
		if(accessToken == null) {
			throw new NoTokenException();
		}
		
		//Create get request
		HttpGet request = new HttpGet(target);
		
		//Add params
		params.add(new BasicNameValuePair("access_token", accessToken));
		URI uri = new URIBuilder(request.getURI()).addParameters(params).build();
		request.setURI(uri);
		
		//Make request
		CloseableHttpResponse response = client.execute(request);		

		//Check for 200 code
		if(response.getStatusLine().getStatusCode() != 200) {
			response.close();
			return null;
		}
		
		//Convert to json
		String jsonString = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
		JsonObject obj = Json.fromJson(jsonString, JsonObject.class);
		
		response.close();
		
		return obj;
	}
	
}
