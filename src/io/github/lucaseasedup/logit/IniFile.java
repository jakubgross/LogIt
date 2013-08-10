package io.github.lucaseasedup.logit;

import com.google.common.collect.ImmutableSet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IniFile
{
    public IniFile(File f) throws IOException
    {
        load(new FileInputStream(f));
    }
    
    public IniFile(String s) throws IOException
    {
        load(new ByteArrayInputStream(s.getBytes()));
    }
    
    public Set<String> getSections()
    {
        return ImmutableSet.copyOf(entries.keySet());
    }
    
    public boolean hasSection(String section)
    {
        return entries.containsKey(section);
    }
    
    public void putSection(String section)
    {
        entries.put(section, new LinkedHashMap<String, String>());
    }
    
    public void putString(String section, String key, String value)
    {
        entries.get(section).put(key, value);
    }
    
    public Set<String> getSectionKeys(String section)
    {
        Map<String, String> kv = entries.get(section);
        
        if (kv == null)
            return null;
        
        return ImmutableSet.copyOf(kv.keySet());
    }
    
    public String getString(String section, String key, String defaultValue)
    {
        Map<String, String> kv = entries.get(section);
        
        if (kv == null)
            return defaultValue;
        
        String v = kv.get(key);
        
        if (v == null)
            return defaultValue;
        
        return v;
    }
    
    public String getString(String section, String key)
    {
        Map<String, String> kv = entries.get(section);
        
        if (kv == null)
            return null;
        
        String v = kv.get(key);
        
        if (v == null)
            return null;
        
        return v;
    }
    
    public int getInt(String section, String key, int defaultValue)
    {
        Map<String, String> kv = entries.get(section);
        
        if (kv == null)
            return defaultValue;

        String v = kv.get(key);
        
        if (v == null)
            return defaultValue;
        
        return Integer.parseInt(kv.get(key));
    }
    
    public float getFloat(String section, String key, float defaultValue)
    {
        Map<String, String> kv = entries.get(section);
        
        if (kv == null)
            return defaultValue;

        String v = kv.get(key);
        
        if (v == null)
            return defaultValue;
        
        return Float.parseFloat(kv.get(key));
    }
    
    public double getDouble(String section, String key, double defaultValue)
    {
        Map<String, String> kv = entries.get(section);
        
        if (kv == null)
            return defaultValue;

        String v = kv.get(key);
        
        if (v == null)
            return defaultValue;
        
        return Double.parseDouble(kv.get(key));
    }
    
    public boolean getBoolean(String section, String key, boolean defaultValue)
    {
        Map<String, String> kv = entries.get(section);
        
        if (kv == null)
            return defaultValue;

        String v = kv.get(key);
        
        if (v == null)
            return defaultValue;
        
        return Boolean.parseBoolean(kv.get(key));
    }
    
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(); 
        
        for (Entry<String, Map<String, String>> section : entries.entrySet())
        {
            sb.append("[");
            sb.append(section.getKey());
            sb.append("]\n");
            
            for (Entry<String, String> kv : section.getValue().entrySet())
            {
                sb.append(kv.getKey());
                sb.append("=");
                sb.append(kv.getValue());
                sb.append("]\n");
            }
            
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    public void save(OutputStream os) throws IOException
    {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os)))
        {
            for (Entry<String, Map<String, String>> section : entries.entrySet())
            {
                bw.write("[" + section.getKey() + "]");
                bw.newLine();
                
                for (Entry<String, String> kv : section.getValue().entrySet())
                {
                    bw.write(kv.getKey() + "=" + kv.getValue());
                    bw.newLine();
                }
                
                bw.newLine();
            }
        }
    }
    
    private void load(InputStream is) throws IOException
    {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is)))
        {
            String line;
            String section = null;
            
            while ((line = br.readLine()) != null)
            {
                Matcher m = sectionPattern.matcher(line);
                
                if (m.matches())
                {
                    section = m.group(1).trim();
                    
                    entries.put(section, new LinkedHashMap<String, String>());
                }
                else if (section != null)
                {
                    m = keyValuePattern.matcher(line);
                    
                    if (m.matches())
                    {
                        String key = m.group(1).trim();
                        String value = m.group(2).trim();
                        
                        entries.get(section).put(key, value);
                    }
                }
            }
        }
    }
    
    private final Pattern sectionPattern = Pattern.compile("\\s*\\[([^]]*)\\]\\s*");
    private final Pattern keyValuePattern = Pattern.compile("\\s*([^=]*)=(.*)");
    private final Map<String, Map<String, String>> entries = new LinkedHashMap<>();
}
