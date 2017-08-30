package android.media;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Administrator on 2015/11/16.
 */
public class AmrEncoder {

    public static void pcm2Amr(String pcmPath , String amrPath) {
        System.out.println("pcm2Amr>>"+pcmPath+">>"+amrPath);
        FileInputStream fis;
        try {
            fis = new FileInputStream(pcmPath);
            pcm2Amr(fis, amrPath);
            fis.close();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void pcm2Amr(InputStream pcmStream, String amrPath) {
        try {
            AmrInputStream ais = new AmrInputStream(pcmStream);
            OutputStream out = new FileOutputStream(amrPath);
            byte[] buf = new byte[4096];
            int len = -1;
            /*
             * 下面的AMR的文件头,缺少这几个字节是不行的
             */
            out.write(0x23);
            out.write(0x21);
            out.write(0x41);
            out.write(0x4D);
            out.write(0x52);
            out.write(0x0A);
            while((len = ais.read(buf)) >0){
                out.write(buf,0,len);
            }
            out.close();
            ais.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
