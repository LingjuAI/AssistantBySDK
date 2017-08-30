package com.lingju.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class AudioXmlParser {

	public static String parseNluResult(String xml)
	{
		StringBuffer buffer = new StringBuffer();
		try
		{
			// DOM builder
			DocumentBuilder domBuilder = null;
			// DOM doc
			Document domDoc = null;

			// init DOM
			DocumentBuilderFactory domFact = DocumentBuilderFactory.newInstance();
			domBuilder = domFact.newDocumentBuilder();
			InputStream is = new ByteArrayInputStream(xml.getBytes("utf-8"));
			domDoc = domBuilder.parse(is);

			// 获取根节�?
			Element root = (Element) domDoc.getDocumentElement();
			
			Element raw = (Element)root.getElementsByTagName("rawtext").item(0);
			buffer.append("【识别结果】" + raw.getFirstChild().getNodeValue());
			buffer.append("\n");
			
			Element e = (Element)root.getElementsByTagName("result").item(0);
			
			Element focus = (Element)e.getElementsByTagName("focus").item(0);
			buffer.append("【FOCUS】" + focus.getFirstChild().getNodeValue());
			buffer.append("\n");
			
			Element action = (Element)e.getElementsByTagName("action").item(0);
			Element operation = (Element)action.getElementsByTagName("operation").item(0);
			buffer.append("【ACTION】"+ operation.getFirstChild().getNodeValue());
			buffer.append("\n");
			

		}catch(Exception e){
			e.printStackTrace();
		};
		buffer.append("\n");
		buffer.append("【ALL】" + xml);
		return buffer.toString();
	}
	static XmlPullParser parser;
	public static String parseXmlResult(String xml,int confidence){
		InputStream inputStream=null;
		try
		{
			inputStream=new ByteArrayInputStream(xml.getBytes());
			if(parser==null) {
				XmlPullParserFactory xpFactory = XmlPullParserFactory.newInstance();
				parser = xpFactory.newPullParser();
			}
			parser.setInput(inputStream, "utf-8");
			int eventType=parser.getEventType();
			String result="";
			while(eventType!= XmlPullParser.END_DOCUMENT){
				if(XmlPullParser.START_TAG==eventType){
					if(parser.getName().equals("rawtext")){
						result=parser.nextText();
					}
					else if(parser.getName().equals("confidence")){
						try{
							if(Integer.parseInt(parser.nextText())>=confidence){
								return result.replaceAll("\r|\n", "");
							}
							return "";
						}catch(Exception e){
							//e.printStackTrace();
						}
					}
				}
				eventType=parser.next();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		finally{
			if(inputStream!=null)
				try {
					inputStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		return "";
	}

	static char AwakenKeys[][]=new char[][]{
			{'小','扫'},
			{'灵','宁','银','明'},
			{'你','您','侬','尼','厘'},
			{'好','奥','呼'}
	};

	public static boolean checkAwakenInXmlResult(String xml,int confidence){
		InputStream inputStream=null;
		try
		{
			inputStream=new ByteArrayInputStream(xml.getBytes());
			if(parser==null) {
				XmlPullParserFactory xpFactory = XmlPullParserFactory.newInstance();
				parser = xpFactory.newPullParser();
			}
			parser.setInput(inputStream, "utf-8");
			int eventType=parser.getEventType();
			boolean bimgo=false;
			while(eventType!= XmlPullParser.END_DOCUMENT){
				if(XmlPullParser.START_TAG==eventType){
					if(parser.getName().equals("rawtext")){
						bimgo=checkAwakenKeywords(parser.nextText());
						if(!bimgo)return false;
					}
					else if(parser.getName().equals("confidence")){
						try{
							if(Integer.parseInt(parser.nextText())>=confidence){
								return bimgo;
							}
							return false;
						}catch(Exception e){
							e.printStackTrace();
						}
					}
				}
				eventType=parser.next();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		finally{
			if(inputStream!=null)
				try {
					inputStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		return false;
	}

	private static boolean checkAwakenKeywords(String text){
		if(text.length()==4){
			boolean r=false;
			char t;
			for(int i=0;i<4;i++){
				t=text.charAt(i);
				for(char c:AwakenKeys[i]){
					r=c==t;
					if(r)break;
				}
				if(!r)return r;
			}
			return r;
		}
		return false;
	}
	
	public static void main(String args[]){
		String xml="<?xml version='1.0' encoding='utf-8' standalone='yes' ?><nlp>\n" +
				"    <version>1.1</version>\n" +
				"    <rawtext>小灵您奥</rawtext>\n" +
				"    <confidence>59</confidence>\n" +
				"    <engine>local</engine>\n" +
				"    <result>\n" +
				"    <focus>小|灵|您|奥</focus>\n" +
				"    <confidence>0|0|0|0</confidence>\n" +
				"    <object>\n" +
				"    <小 id=\"65535\">小</小>\n" +
				"    <灵 id=\"65535\">灵</灵>\n" +
				"    <您 id=\"65535\">您</您>\n" +
				"    <奥 id=\"65535\">奥</奥>\n" +
				"    </object>\n" +
				"    </result>\n" +
				"    </nlp>";
		System.out.println(checkAwakenKeywords("小灵您奥"));
	}
}
