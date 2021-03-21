

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.Excluder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class Json {

	private static final Gson GSON;
	
	static {
		GsonBuilder builder = new GsonBuilder();	
		builder.setPrettyPrinting();
		GSON = builder.create();
	}

	/**
	 * @return
	 * @see com.google.gson.Gson#newBuilder()
	 */
	public static GsonBuilder newBuilder() {
		return GSON.newBuilder();
	}

	/**
	 * @return
	 * @see com.google.gson.Gson#excluder()
	 */
	public static Excluder excluder() {
		return GSON.excluder();
	}

	/**
	 * @return
	 * @see com.google.gson.Gson#fieldNamingStrategy()
	 */
	public static FieldNamingStrategy fieldNamingStrategy() {
		return GSON.fieldNamingStrategy();
	}

	/**
	 * @return
	 * @see com.google.gson.Gson#serializeNulls()
	 */
	public static boolean serializeNulls() {
		return GSON.serializeNulls();
	}

	/**
	 * @return
	 * @see com.google.gson.Gson#htmlSafe()
	 */
	public static boolean htmlSafe() {
		return GSON.htmlSafe();
	}

	/**
	 * @param <T>
	 * @param type
	 * @return
	 * @see com.google.gson.Gson#getAdapter(com.google.gson.reflect.TypeToken)
	 */
	public static <T> TypeAdapter<T> getAdapter(TypeToken<T> type) {
		return GSON.getAdapter(type);
	}

	/**
	 * @param <T>
	 * @param skipPast
	 * @param type
	 * @return
	 * @see com.google.gson.Gson#getDelegateAdapter(com.google.gson.TypeAdapterFactory, com.google.gson.reflect.TypeToken)
	 */
	public static <T> TypeAdapter<T> getDelegateAdapter(TypeAdapterFactory skipPast, TypeToken<T> type) {
		return GSON.getDelegateAdapter(skipPast, type);
	}

	/**
	 * @param <T>
	 * @param type
	 * @return
	 * @see com.google.gson.Gson#getAdapter(java.lang.Class)
	 */
	public static <T> TypeAdapter<T> getAdapter(Class<T> type) {
		return GSON.getAdapter(type);
	}

	/**
	 * @param src
	 * @return
	 * @see com.google.gson.Gson#toJsonTree(java.lang.Object)
	 */
	public static JsonElement toJsonTree(Object src) {
		return GSON.toJsonTree(src);
	}

	/**
	 * @param src
	 * @param typeOfSrc
	 * @return
	 * @see com.google.gson.Gson#toJsonTree(java.lang.Object, java.lang.reflect.Type)
	 */
	public static JsonElement toJsonTree(Object src, Type typeOfSrc) {
		return GSON.toJsonTree(src, typeOfSrc);
	}

	/**
	 * @param src
	 * @return
	 * @see com.google.gson.Gson#toJson(java.lang.Object)
	 */
	public static String toJson(Object src) {
		return GSON.toJson(src);
	}

	/**
	 * @param src
	 * @param typeOfSrc
	 * @return
	 * @see com.google.gson.Gson#toJson(java.lang.Object, java.lang.reflect.Type)
	 */
	public static String toJson(Object src, Type typeOfSrc) {
		return GSON.toJson(src, typeOfSrc);
	}

	/**
	 * @param src
	 * @param writer
	 * @throws JsonIOException
	 * @see com.google.gson.Gson#toJson(java.lang.Object, java.lang.Appendable)
	 */
	public static void toJson(Object src, Appendable writer) throws JsonIOException {
		GSON.toJson(src, writer);
	}

	/**
	 * @param src
	 * @param typeOfSrc
	 * @param writer
	 * @throws JsonIOException
	 * @see com.google.gson.Gson#toJson(java.lang.Object, java.lang.reflect.Type, java.lang.Appendable)
	 */
	public static void toJson(Object src, Type typeOfSrc, Appendable writer) throws JsonIOException {
		GSON.toJson(src, typeOfSrc, writer);
	}

	/**
	 * @param src
	 * @param typeOfSrc
	 * @param writer
	 * @throws JsonIOException
	 * @see com.google.gson.Gson#toJson(java.lang.Object, java.lang.reflect.Type, com.google.gson.stream.JsonWriter)
	 */
	public static void toJson(Object src, Type typeOfSrc, JsonWriter writer) throws JsonIOException {
		GSON.toJson(src, typeOfSrc, writer);
	}

	/**
	 * @param jsonElement
	 * @return
	 * @see com.google.gson.Gson#toJson(com.google.gson.JsonElement)
	 */
	public static String toJson(JsonElement jsonElement) {
		return GSON.toJson(jsonElement);
	}

	/**
	 * @param jsonElement
	 * @param writer
	 * @throws JsonIOException
	 * @see com.google.gson.Gson#toJson(com.google.gson.JsonElement, java.lang.Appendable)
	 */
	public static void toJson(JsonElement jsonElement, Appendable writer) throws JsonIOException {
		GSON.toJson(jsonElement, writer);
	}

	/**
	 * @param writer
	 * @return
	 * @throws IOException
	 * @see com.google.gson.Gson#newJsonWriter(java.io.Writer)
	 */
	public static JsonWriter newJsonWriter(Writer writer) throws IOException {
		return GSON.newJsonWriter(writer);
	}

	/**
	 * @param reader
	 * @return
	 * @see com.google.gson.Gson#newJsonReader(java.io.Reader)
	 */
	public static JsonReader newJsonReader(Reader reader) {
		return GSON.newJsonReader(reader);
	}

	/**
	 * @param jsonElement
	 * @param writer
	 * @throws JsonIOException
	 * @see com.google.gson.Gson#toJson(com.google.gson.JsonElement, com.google.gson.stream.JsonWriter)
	 */
	public static void toJson(JsonElement jsonElement, JsonWriter writer) throws JsonIOException {
		GSON.toJson(jsonElement, writer);
	}

	/**
	 * @param <T>
	 * @param json
	 * @param classOfT
	 * @return
	 * @throws JsonSyntaxException
	 * @see com.google.gson.Gson#fromJson(java.lang.String, java.lang.Class)
	 */
	public static <T> T fromJson(String json, Class<T> classOfT) throws JsonSyntaxException {
		return GSON.fromJson(json, classOfT);
	}

	/**
	 * @param <T>
	 * @param json
	 * @param typeOfT
	 * @return
	 * @throws JsonSyntaxException
	 * @see com.google.gson.Gson#fromJson(java.lang.String, java.lang.reflect.Type)
	 */
	public static <T> T fromJson(String json, Type typeOfT) throws JsonSyntaxException {
		return GSON.fromJson(json, typeOfT);
	}

	/**
	 * @param <T>
	 * @param json
	 * @param classOfT
	 * @return
	 * @throws JsonSyntaxException
	 * @throws JsonIOException
	 * @see com.google.gson.Gson#fromJson(java.io.Reader, java.lang.Class)
	 */
	public static <T> T fromJson(Reader json, Class<T> classOfT) throws JsonSyntaxException, JsonIOException {
		return GSON.fromJson(json, classOfT);
	}

	/**
	 * @param <T>
	 * @param json
	 * @param typeOfT
	 * @return
	 * @throws JsonIOException
	 * @throws JsonSyntaxException
	 * @see com.google.gson.Gson#fromJson(java.io.Reader, java.lang.reflect.Type)
	 */
	public static <T> T fromJson(Reader json, Type typeOfT) throws JsonIOException, JsonSyntaxException {
		return GSON.fromJson(json, typeOfT);
	}

	/**
	 * @param <T>
	 * @param reader
	 * @param typeOfT
	 * @return
	 * @throws JsonIOException
	 * @throws JsonSyntaxException
	 * @see com.google.gson.Gson#fromJson(com.google.gson.stream.JsonReader, java.lang.reflect.Type)
	 */
	public static <T> T fromJson(JsonReader reader, Type typeOfT) throws JsonIOException, JsonSyntaxException {
		return GSON.fromJson(reader, typeOfT);
	}

	/**
	 * @param <T>
	 * @param json
	 * @param classOfT
	 * @return
	 * @throws JsonSyntaxException
	 * @see com.google.gson.Gson#fromJson(com.google.gson.JsonElement, java.lang.Class)
	 */
	public static <T> T fromJson(JsonElement json, Class<T> classOfT) throws JsonSyntaxException {
		return GSON.fromJson(json, classOfT);
	}

	/**
	 * @param <T>
	 * @param json
	 * @param typeOfT
	 * @return
	 * @throws JsonSyntaxException
	 * @see com.google.gson.Gson#fromJson(com.google.gson.JsonElement, java.lang.reflect.Type)
	 */
	public static <T> T fromJson(JsonElement json, Type typeOfT) throws JsonSyntaxException {
		return GSON.fromJson(json, typeOfT);
	}
	
	
	
}
