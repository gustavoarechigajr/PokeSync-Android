using System.Text.Json;
using System.Text.Json.Serialization;

public class ByteArrayJsonConverter : JsonConverter<byte[]>
{
    public override byte[] Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        if (reader.TokenType == JsonTokenType.StartArray)
        {
            var list = new List<byte>();
            while (reader.Read() && reader.TokenType != JsonTokenType.EndArray)
            {
                list.Add(reader.GetByte());
            }
            return list.ToArray();
        }

        return reader.GetBytesFromBase64();
    }

    public override void Write(Utf8JsonWriter writer, byte[] value, JsonSerializerOptions options)
    {
        writer.WriteStartArray();
        foreach (var b in value)
        {
            writer.WriteNumberValue(b);
        }
        writer.WriteEndArray();
    }
}
