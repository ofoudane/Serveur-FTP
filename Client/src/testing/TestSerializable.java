package testing;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import model.query.Query;

public class TestSerializable {
   public static void main(String args[]) throws Exception {
	   testBuffer();
   }
   
   public static void testQuery1() {
	   Query myQuery = new Query("Omar", 1);
	   myQuery.dataAsString();
	   byte[] b = myQuery.toBytes();
	   Query secondQuery = new Query();
	   secondQuery.fromBytes(b);
	   System.out.println(secondQuery.dataAsString());
	   System.out.println(secondQuery.equals(myQuery));
	   
   }

   public static void printArray(byte[] arr) {
	   for(int i = 0;i < arr.length; i++) {
		   System.out.print(arr[i]);
	   }
	   System.out.println();
   }
   
   public static void testBuffer() {
	   
	   ByteBuffer buffer = ByteBuffer.allocate(4);
	   
	   buffer.order(ByteOrder.LITTLE_ENDIAN);
	   
	   buffer.put("abcd".getBytes());
	   
	   System.out.println(Arrays.toString(buffer.array()));
	   
   }
}