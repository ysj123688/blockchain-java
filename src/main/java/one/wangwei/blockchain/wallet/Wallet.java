package one.wangwei.blockchain.wallet;

import lombok.AllArgsConstructor;
import lombok.Data;
import one.wangwei.blockchain.crypto.Base58Check;
import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.util.Arrays;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.security.*;

/**
 * 钱包
 *
 * @author wangwei
 * @date 2018/03/14
 */
@Data
@AllArgsConstructor
public class Wallet implements Serializable {

    /**
     * 校验码长度
     */
    private static final int ADDRESS_CHECKSUM_LEN = 4;
    /**
     * 私钥
     */
    private PrivateKey privateKey;
    /**
     * 公钥
     */
    private PublicKey publicKey;


    public Wallet() {
        initWallet();
    }

    /**
     * 初始化钱包
     */
    private void initWallet() {
        try {
            KeyPair keyPair = newKeyPair();
            this.privateKey = keyPair.getPrivate();
            this.publicKey = keyPair.getPublic();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建新的密钥对
     *
     * @return
     * @throws Exception
     */
    private KeyPair newKeyPair() throws Exception {
        // 注册 BC Provider
        Security.addProvider(new BouncyCastleProvider());
        // 创建椭圆曲线算法的密钥对生成器，算法为 ECDSA
        KeyPairGenerator g = KeyPairGenerator.getInstance("ECDSA", BouncyCastleProvider.PROVIDER_NAME);
        // 椭圆曲线（EC）域参数设定
        // bitcoin 为什么会选择 secp256k1，详见：https://bitcointalk.org/index.php?topic=151120.0
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");
        g.initialize(ecSpec, new SecureRandom());
        return g.generateKeyPair();
    }


    /**
     * 获取钱包地址
     *
     * @return
     */
    public String getAddress() throws Exception {
        //1. 先对公钥做 sha256 处理
        byte[] shaHashedKey = DigestUtils.sha256(this.getPublicKey().getEncoded());

        //2. 再执行 RIPEMD-160 hash 处理
        byte[] ripemdHashedKey = ripeMD160Hash(shaHashedKey);

        //3. 添加版本 0x00
        ByteArrayOutputStream addrStream = new ByteArrayOutputStream();
        addrStream.write((byte) 0);
        addrStream.write(ripemdHashedKey);
        byte[] versionedPayload = addrStream.toByteArray();

        //4. 计算校验码
        byte[] checksum = checksum(versionedPayload);

        //5. 得到 version + paylod + checksum 的组合
        addrStream.write(checksum);
        byte[] binaryAddress = addrStream.toByteArray();

        //6. 执行Base58转换处理
        return Base58Check.rawBytesToBase58(binaryAddress);
    }


    /**
     * 计算 RIPEMD160 Hash值
     *
     * @param pubKey
     * @return
     */
    private byte[] ripeMD160Hash(byte[] pubKey) {
        RIPEMD160Digest ripemd160 = new RIPEMD160Digest();
        ripemd160.update(pubKey, 0, pubKey.length);
        byte[] output = new byte[ripemd160.getDigestSize()];
        ripemd160.doFinal(output, 0);
        return output;
    }


    /**
     * 生成公钥的校验码
     *
     * @param payload
     * @return
     */
    public byte[] checksum(byte[] payload) {
        byte[] firstSHA = DigestUtils.sha256(payload);
        byte[] secondSHA = DigestUtils.sha256(firstSHA);
        return Arrays.copyOfRange(secondSHA, 0, 4);
    }

}
