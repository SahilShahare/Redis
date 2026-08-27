package Components;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class RespSerializer {

    public String serializeBulkString(String s) {
        int length = s.length();
        String respHeader = "$"+length;
        return respHeader + "\r\n" + s + "\r\n";
    }

    public List<String[]>  deserialize(byte[] command){

        String data = new String(command, StandardCharsets.UTF_8);
        char[] dataChars = data.toCharArray();

        List<String[]> res = new ArrayList<>();
        int i=0;
        while(i < dataChars.length) {
            char curr = dataChars[i];
            if(curr=='\u0000')
                //command ends with null character
                break;
            else if(curr == '*'){
                i++;
                StringBuilder arrLen = new StringBuilder();
                
                i = getInteger(dataChars, i, arrLen);
                
                if(dataChars[i] == '*'){
                    //Multiple command : Eg. *2\r\n*3\r\n$3\r\nset\r\n$3\r\nkey\r\n$5\r\nvalue\r\n*2\r\n$3\r\nget\r\n$3\r\nkey\r\n
                    for(int c=0; c<Integer.parseInt(arrLen.toString()); c++) {
                        i++;
                        StringBuilder nestedLen = new StringBuilder();
                        
                        i= getInteger(dataChars, i, nestedLen);
                        
                        String[] subArray = new String[Integer.parseInt(nestedLen.toString())];
                        i = getCommand(dataChars, i, subArray);
                        res.add(subArray);
                    }
                } else {
                    //Single command : Eg. *3\r\n$3\r\nset\r\n$3\r\nkey\r\n$5\r\nvalue\r\n
                    String[] subArray = new String[Integer.parseInt(arrLen.toString())];
                    i = getCommand(dataChars, i, subArray);
                    res.add(subArray);
                }

            }

        }

        return res;
    }

    private int getCommand(char[] dataChars, int i, String[] subArray){
        int j = 0;
        while(i<dataChars.length && j< subArray.length) {
            if(dataChars[i] == '$'){
                //Bulk String : $<length>\r\n<data>\r\n
                i++;
                StringBuilder partLength = new StringBuilder();
                i = getInteger(dataChars, i, partLength);

                StringBuilder part = new StringBuilder();
                for(int k =0; k<Integer.parseInt(partLength.toString()); k++){
                    part.append(dataChars[i++]);
                }
                i+=2;
                subArray[j++] = part.toString();

            }
        }
        return i;
    }

    private int getInteger(char[] dataChars, int i, StringBuilder arrLen) {
        while(i < dataChars.length && Character.isDigit(dataChars[i])){
            arrLen.append(dataChars[i++]);
        }
        i+=2;
        return i;
    }
}
