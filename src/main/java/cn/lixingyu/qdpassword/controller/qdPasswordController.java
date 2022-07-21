package cn.lixingyu.qdpassword.controller;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lxxxxxxy
 * @time 2021/10/26 13:37
 */
@Controller
public class qdPasswordController {

    private static Cipher cipher = null;
    private static boolean isInit = false;
    private static final Map<Byte, Byte> decodeMap = new HashMap();
    private static final Map<Byte, Byte> encodeMap = new HashMap();
    private static final byte[] sourceChar = {65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 124, 46, 45, 43, 47, 44, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 123, 125, 126, 91, 92, 93, 94, 95, 58, 59, 60, 61, 62, 63, 64};
    private static final byte[] targetChar = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 43, 35, 124, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 64, 36, 61, 34, 47, 33, 37, 38, 39, 40, 41, 42, 45, 46, 123, 125, 126, 91, 92, 93, 94, 95, 58, 59, 60, 62, 63, 44};

    @RequestMapping("/")
    public String getPassword(Model modelMap) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
//        OkHttpClient client = new OkHttpClient().newBuilder()
//                .build();
//        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
//        RequestBody body = RequestBody.create(mediaType, "userToken=v1_UU5LQm56KzdLYnJZSGl6RnhVUmd1VlNucGpRYmZ1bVN1QW1yTW1qbDE4ZW9xUG93Si8vSGlES29Oc2puSERkaDRXTXo5a0NzQUFYRnJSZEtyN0FYVHFTcjZkZU10ZWNQSTlXbFozcVlocE9vMGJCbFhVdWloY2FzejZFSlcyMmwzbGhpTURUQXJHWit0UzhwU2o3a1VOOGl6czZFT2orUUEyRXhNRG83TzltdzlRd2Nqd1UvTTlDTkVOUEEwdnhudW4xMFpRNE11bTJCVzB0K3pWWUovM213VndTTEFCUUZYMW9zcnRXUGo0b1NWUWFFdGRleWZId05MQWhHV3VQdEhON0pxYm1PdWtLTTQySGVLd1dpek94OE14OTF5endWaDhidDFKMURtTGw4bzhjOU5JMlcxRFVLVkhHRTh6ODh1Zk5Td0M5OUI5UT0MkwG92kOZf_MS4lgUPAkrHA_amBUFtRAUS2wCZxKi2JuozOZiIPfY6L9HPOGV4xFiaNawQHmUoaaI63pY_1BXjYuT93YEliXxoF_PQWbKF_UorS03AKJZVUOMoAWIfEzVDrpSg36CK6irTY_loYOxLtCwdAqMfqkoJrbHc-4XoQ..&body=gscITj0SCFUnUPwU-fG12RhQss7SGLwzhceN0XTt9zIUOl_hftxwHICjRG0luRDUpDl9utzpfjdNKt4ny8daliqZT_0syEI8qoMiqjrnKbu0mYFph7hZV6jlHZjtbCDEUz0R3jDAmTUpAWuIEnOn1MP6wQb9AlFPaDhvnVsyuDko5ijZvPqGn-owxQ4vloZBXcTkaIzYrufmkdaU-jrDJtr6yNplaMHTsxy4kPhuCj0V0GKVrdgz1sripiHbvY08SlqbZ098bGf6mB-4B-r4JvfGWfzUXuLV3tSGrKIyiChx7RwZ9VWU32IUuvn6d0U01feND5P_rurciM-o8NPoKpcMLeDJH-2km8ySq06GRcX7d8SqoksZCYNzeRWjCZ0FcSg0M_QsJrFS35YTb8V18oSIGFJSiML82Asbuw8MYQYsBRADOhRFCRwR70-s0gnZFImefKxUzm5RdDZ28HbnPCmf9lj08USXTEPG0bZCT-7gXDqndYnccrv8JZxUEvEK-LHYL5ZWswxpR1ysCYRzqkU37Rs1lTDZCLuY8A3-T3k0NZAtyWjrVBw2EG_q5V2a&timestamp=1635224556545&signCode=47aa87e7e85bb56e89bfd1c296b8a19e");
//        Request request = new Request.Builder()
//                .url("https://api.qdingnet.com/qding-api/api/json/housekeeper/applyAccessPasswordForHost")
//                .method("POST", body)
//                .addHeader("Accept-Language", " zh-CN,zh;q=0.8")
//                .addHeader("User-Agent", " Mozilla/5.0 (Linux; U; Android 11; zh-cn; MEIZU 18 Build/RKQ1.210715.001) AppleWebKit/533.1 (KHTML, like Gecko) Version/5.0 Mobile Safari/533.1")
//                .addHeader("transport-security-token", "txv1vdaz66x6abvt")
//                .addHeader("Content-Type", "application/x-www-form-urlencoded")
//                .addHeader("Host", " api.qdingnet.com")
//                .addHeader("Connection", " Keep-Alive")
//                .addHeader("Accept-Encoding", " gzip")
//                .addHeader("Content-Length", " 1261")
//                .build();
//        Response response = client.newCall(request).execute();

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("http://api.qdingnet.com/qding-api/api/json/housekeeper/applyAccessPasswordForHost"); // 创建httpget实例
        httpPost.addHeader("Accept-Language", "zh-CN,zh;q=0.8");
        httpPost.addHeader("User-Agent", "Mozilla/5.0 (Linux; U; Android 11; zh-cn; MEIZU 18 Build/RKQ1.210715.001) AppleWebKit/533.1 (KHTML, like Gecko) Version/5.0 Mobile Safari/533.1");
        httpPost.addHeader("transport-security-token", "xff66rsmeeyiv3f2");
        httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
        httpPost.addHeader("Host", "api.qdingnet.com");
        httpPost.addHeader("Connection", "Keep-Alive");
        httpPost.addHeader("Accept-Encoding", "gzip");
//        httpPost.addHeader("Content-Length", "1261");
//userToken=v1_UU5LQm56KzdLYnJZSGl6RnhVUmd1VlNucGpRYmZ1bVN1QW1yTW1qbDE4ZW9xUG93Si8vSGlES29Oc2puSERkaDRXTXo5a0NzQUFYRnJSZEtyN0FYVHFTcjZkZU10ZWNQSTlXbFozcVlocE9vMGJCbFhVdWloY2FzejZFSlcyMmwzbGhpTURUQXJHWit0UzhwU2o3a1VOOGl6czZFT2orUUEyRXhNRG83TzltdzlRd2Nqd1UvTTlDTkVOUEEwdnhudW4xMFpRNE11bTJCVzB0K3pWWUovM213VndTTEFCUUZYMW9zcnRXUGo0b1NWUWFFdGRleWZId05MQWhHV3VQdEhON0pxYm1PdWtLTTQySGVLd1dpek94OE14OTF5endWb0dReDU2Vy8rV3RlSDFFYk5iL2t2VFVLVkhHRTh6ODh1Zk5Td0M5OUI5UT1pn9BwszCAjd5nJ3KFXyEEM4HnVlz57G2OmOg_b46q7b0UKZR3jKEzUMBtCEdgO6EGK4jDma75b9i8QcdqjMQ65PiLvKvm_hUiC2JMpXJ7HzIErVH856kBq6gbYn__wNEjJ3JPwlrPFIvYQH6TrHRv0uKj6iWoeRNdLzQBwDqNcA..&body=Fxe0235qJQc4HrINunUADLEZr3eXSKca1SqCCWIzr02yLE7krD0lyNz22BI2wqZ0mzX3nt-9U4Ea6cD9-HFSJrwqjl1fc8GbxHzERQIahvCQn3q7rzi9eL7cPOtVN__MBrZFKhgq2RrYP0krFtUw37igMbru2D4YUnZt35VckY3ghxmZbqBIm8lf92z-jhXpZAD7fX-KzV9CPkdCERe4XnmLzJAgUCkeIHO3boRve2jD5Vu03_0n2f0o1otnSi394UMKmMNxpx441yAzmy5N829kis65226E2J_WH79araM.&timestamp=1636607464833&signCode=13f69bb850552d8cb039014bc959fcc3
        //下面这一行主要是使用抓包工具抓出cookie，对应链接是http://api.qdingnet.com/qding-api/api/json/housekeeper/applyAccessPasswordForHost，cookie有效期大概20多天
        StringEntity stringEntity = new StringEntity("userToken=v1_UU5LQm56KzdLYnJZSGl6RnhVUmd1VlNucGpRYmZ1bVN1QW1yTW1qbDE4ZW9xUG93Si8vSGlES29Oc2puSERkaDRXTXo5a0NzQUFYRnJSZEtyN0FYVHFTcjZkZU10ZWNQSTlXbFozcVlocE9vMGJCbFhVdWloY2FzejZFSlcyMmwzbGhpTURUQXJHWit0UzhwU2o3a1VOOGl6czZFT2orUUEyRXhNRG83TzltdzlRd2Nqd1UvTTlDTkVOUEEwdnhudW4xMFpRNE11bTJCVzB0K3pWWUovM213VndTTEFCUUZYMW9zcnRXUGo0b1NWUWFFdGRleWZId05MQWhHV3VQdEhON0pxYm1PdWtLTTQySGVLd1dpek94OE14OTF5endWYkREZWdYdFlMN2hhOFY0WTc2RnRtRFVLVkhHRTh6ODh1Zk5Td0M5OUI5UT1AMIGrCqbuHD4cg8N21uymylBX8NrBKDnjPyIh8R3vaumjuj5MLlSS56sjZ0kP8lIkeQQBkpfDUAjkzCNUpmEMha-13G2BG0SLVuomU3YCbE_jmps0LliE-9bdBSyJq4cOZv0l3ihUAO894KDE7YUMB8fAqS5vtcWUJe1WZZ4Tzw..&body=fwmCfEH1EJAe7UNYRM7jGBhKM3pXH3cNOMCRO2zfSdDvvGZdaXBbj_9krc3nPDS2EX3zhBFfe92SscCUJDDWG7tlltMvNgYmhBDdOENbLE-GnuMVE3mIvQ-cePtHjaQCi87LOK4jsqcQB2bzK5__UKOwd3r6U03gIvU2zMb0iCjEHVYxeVS3rbr2kZw879PqUHUJ15taM5mObxheu4olsfUEXbdAIaAhF9XrMmTbhV_iDTBXHN7VdebRQQOeeLSutdgRPkdTzZPuRNSJh3bI4eZelljPcVHrycgAhV3DhqQnkMAyQZrlMdICPjfWx7B7xMZ4ZYKWM7lZ-6ekZNx-LaBxuMHedkotXFWKbu9DVC1zgqkEF2bp4W56jXcoNaMplR3wmNJSQeIO332htycsMA..&timestamp="+System.currentTimeMillis()+"&signCode=1f7a2e1d40a5abea57b66d915a5cf39d");
        httpPost.setEntity(stringEntity);
        CookieStore cookieStore = new BasicCookieStore();
        httpClient = HttpClients.custom().setDefaultCookieStore(cookieStore).build();
        CloseableHttpResponse response1 = httpClient.execute(httpPost);
        int i2 = 0;
        HttpEntity entity = response1.getEntity();  //获取网页内容
        String str = EntityUtils.toString(entity, "UTF-8");
        Header[] headers = response1.getHeaders("transport-security-token");
        String str2 = headers[0].toString().split(" ")[1];
        while (true) {
            byte[] bArr = sourceChar;
            if (i2 < bArr.length) {
                if (encodeMap.containsKey(Byte.valueOf(bArr[i2]))) {
                }
                if (decodeMap.containsKey(Byte.valueOf(targetChar[i2]))) {
                }
                encodeMap.put(Byte.valueOf(sourceChar[i2]), Byte.valueOf(targetChar[i2]));
                decodeMap.put(Byte.valueOf(targetChar[i2]), Byte.valueOf(sourceChar[i2]));
                i2++;
            } else {
                break;
            }
        }
        str2 = decode(str2);
        if (str2 == null) {
            try {
                System.out.print("Key为空null");
            } catch (Exception e2) {
                System.out.println(e2.toString());
            }
        } else if (str2.length() != 16) {
            System.out.print("Key长度不是16位");
        } else {
            SecretKeySpec secretKeySpec = new SecretKeySpec(str2.getBytes("UTF-8"), "AES");
            Cipher encrypt = getEncrypt();
            IvParameterSpec ivParameterSpec = new IvParameterSpec("0102030405060708".getBytes());
            if (encrypt != null) {
                encrypt.init(2, secretKeySpec, ivParameterSpec);
            }
            byte[] decodeBuffer = new Base64().decode(prepareBeforeBase64Decode(str));
            if (encrypt == null) {
            }
            try {
                String s = new String(encrypt.doFinal(decodeBuffer)).split("accessPassWord\":\"")[1];
                s.substring(0,6);
                modelMap.addAttribute("password",s.substring(0,6));
                return "index";
            } catch (Exception e3) {
                System.out.println(e3.toString());
            }
        }
        modelMap.addAttribute("error","错误！");
        return "index";
    }

    private static Cipher getEncrypt() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        if (cipher == null && !isInit) {
            if (cipher == null && !isInit) {
                cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                isInit = true;
            }
        }
        return cipher;
    }

    private static String prepareAfterBase64Encode(String str) {
        String replaceAll;
        replaceAll = str.replace("+", "-").replace("/", "_").replace("=", ".").replaceAll("(\r\n|\n\r|\r|\n)", "");
        return replaceAll;
    }

    private static synchronized String prepareBeforeBase64Decode(String str) {
        String replace;
        replace = str.replace("-", "+").replace("_", "/").replace(".", "=");
        return replace;
    }


    public static String decode(String str) throws UnsupportedEncodingException {
        byte[] bytes = str.getBytes();
        byte[] bArr = new byte[bytes.length];
        for (int i2 = 0; i2 < bytes.length; i2++) {
            Byte b2 = decodeMap.get(Byte.valueOf(bytes[i2]));
            if (b2 != null) {
                bArr[i2] = b2.byteValue();
            } else {
                throw new UnsupportedEncodingException("CharMapping decode error, char must in [33, 126]");
            }
        }
        return new String(bArr);
    }

}
