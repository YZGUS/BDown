package com.yz.bdown.utils;

import static android.graphics.Bitmap.Config.RGB_565;
import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;
import static com.google.zxing.BarcodeFormat.QR_CODE;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class QRCodeUtil {

    /**
     * 将文本生成二维码 Bitmap
     *
     * @param text   二维码内容
     * @param width  二维码宽度
     * @param height 二维码高度
     * @return 生成的二维码 Bitmap
     */
    public static Bitmap generateQRCodeBitmap(String text, int width, int height) {
        try {
            final QRCodeWriter qrCodeWriter = new QRCodeWriter();
            final BitMatrix bitMatrix = qrCodeWriter.encode(text, QR_CODE, width, height);
            return bitMatrixToBitmap(bitMatrix);    // 将 BitMatrix 转换为 Bitmap
        } catch (Throwable t) {
            Log.e("qr_code", "生成二维码失败", t);
            return null;
        }
    }

    /**
     * 将 BitMatrix 转换为 Bitmap
     *
     * @param bitMatrix 二维码矩阵
     * @return 转换后的 Bitmap
     */
    private static Bitmap bitMatrixToBitmap(BitMatrix bitMatrix) {
        final int width = bitMatrix.getWidth();
        final int height = bitMatrix.getHeight();
        final Bitmap bitmap = Bitmap.createBitmap(width, height, RGB_565);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? BLACK : WHITE);
            }
        }
        return bitmap;
    }
}