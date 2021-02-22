package localapp;

import amazontools.EC2Handler;
import amazontools.S3Handler;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

public class Test {
    public static void main(String[] args) {
        EC2Handler ec2=new EC2Handler();
        boolean bool=ec2.tagExists("manager");
        System.out.println(bool);
        S3Handler s3=new S3Handler();
        s3.deleteAllBuckets();
    }
}
