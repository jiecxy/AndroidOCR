package com.android.kaka;

import java.io.File;
import java.io.FileNotFoundException;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.mikewong.tool.kaka.R;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final int PHOTO_CAPTURE = 0x11;// 拍照
	private static final int PHOTO_RESULT = 0x12;// 结果

	private static String LANGUAGE = "eng";
	private static String IMG_PATH = getSDPath() + java.io.File.separator + "ocrtest";
	boolean ifBinary = false;
	private static EditText tvResult;
	private static ImageView ivSelected;
	private static ImageView ivTreated;
	private static Button btnCamera;
	private static Button btnSelect;
	private static Button btnBaidu;
	private static Button chPreTreat;
	private static RadioGroup radioGroup;
	private static String textResult;
	private static Bitmap bitmapSelected;
	private static Bitmap bitmapTreated;
	private static final int SHOWRESULT = 0x101;
	private static final int SHOWTREATEDIMG = 0x102;
	private ImageButton btn = null;
	private Button cancel = null;
	private LinearLayout contentView = null;
	private Dialog dialog = null;
	// 该handler用于处理修改结果的任务
	public static Handler myHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case SHOWRESULT:
				if (textResult.equals(""))
					tvResult.setText("识别失败");
				else
					tvResult.setText(textResult);
				break;
			case SHOWTREATEDIMG:
				tvResult.setText("识别中......");
				showPicture(ivTreated, bitmapTreated);
				break;
			}
			super.handleMessage(msg);
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Toast.makeText(getApplicationContext(), "默认的Toast", Toast.LENGTH_SHORT).show();
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);

		// 若文件夹不存在 首先创建文件夹
		File path = new File(IMG_PATH);
		if (!path.exists()) {
			path.mkdirs();
		}

		tvResult = (EditText) findViewById(R.id.tv_result);
		ivSelected = (ImageView) findViewById(R.id.iv_selected);
		ivTreated = (ImageView) findViewById(R.id.iv_treated);

		contentView =  (LinearLayout) getLayoutInflater().inflate(R.layout.dialog, null);
		btnCamera = (Button) contentView.findViewById(R.id.btn_camera);
		btnSelect = (Button) contentView.findViewById(R.id.btn_select);
		//btnBaidu = (Button) contentView.findViewById(R.id.btn_baidu);
		cancel = (Button) contentView.findViewById(R.id.cancel);
		cancel.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				dialog.cancel();
			}
		});
		btn = (ImageButton) findViewById(R.id.btn);
		dialog = new Dialog(MainActivity.this);
		dialog.setTitle("请选择");
		dialog.setContentView(contentView);
		btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				dialog.show();
			}
		});

		chPreTreat = (Button) findViewById(R.id.ch_pretreat);
		radioGroup = (RadioGroup) findViewById(R.id.radiogroup);

		btnCamera.setOnClickListener(new cameraButtonListener());
		btnSelect.setOnClickListener(new selectButtonListener());
		// 用于设置解析语言
		radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				switch (checkedId) {
				case R.id.rb_en:
					LANGUAGE = "eng";
					break;
				case R.id.rb_ch:
					LANGUAGE = "chi_sim";
					break;
				}
			}

		});

	}

	public void binaryprocess(View v)
	{
		ifBinary = !ifBinary;
		if(ifBinary)
			chPreTreat.setBackgroundColor(Color.parseColor("#faebd7"));
		else
			chPreTreat.setBackgroundColor(Color.parseColor("#ffffff"));
	}
	
	public void baidu(View v)
	{
		Intent intent= new Intent();        
	    intent.setAction("android.intent.action.VIEW");    
	    Uri content_url = Uri.parse("http://www.baidu.com/s?ie=utf-8&f=8&rsv_bp=0&rsv_idx=1&tn=baidu&wd="
	    			+ tvResult.getText().toString());   
	    intent.setData(content_url);  
	    startActivity(intent);
	}
	
	public void share(View v)
	{
		Intent shareIntent = new Intent();
		//设置Intent的action
		shareIntent.setAction(Intent.ACTION_SEND);
		//设定分享的类型是纯文本的
		shareIntent.setType("text/plain");
		//设置分享主题
		shareIntent.putExtra(Intent.EXTRA_SUBJECT, "分享");
		//设置分享的文本
		shareIntent.putExtra(Intent.EXTRA_TEXT, "" + tvResult.getText().toString());
		startActivity(shareIntent);
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode == Activity.RESULT_CANCELED)
			return;

		if (requestCode == PHOTO_CAPTURE) {
			tvResult.setText("abc");
			startPhotoCrop(Uri.fromFile(new File(IMG_PATH, "temp.jpg")));
		}

		// 处理结果
		if (requestCode == PHOTO_RESULT) {
			bitmapSelected = decodeUriAsBitmap(Uri.fromFile(new File(IMG_PATH,
					"temp_cropped.jpg")));
			if (ifBinary)
				tvResult.setText("预处理中......");
			else
				tvResult.setText("识别中......");
			// 显示选择的图片
			showPicture(ivSelected, bitmapSelected);

			// 新线程来处理识别
			new Thread(new Runnable() {
				@Override
				public void run() {
					if (ifBinary) {
						bitmapTreated = ImgPretreatment
								.doPretreatment(bitmapSelected);
						Message msg = new Message();
						msg.what = SHOWTREATEDIMG;
						myHandler.sendMessage(msg);
						textResult = doOcr(bitmapTreated, LANGUAGE);
					} else {
						bitmapTreated = ImgPretreatment
								.converyToGrayImg(bitmapSelected);
						Message msg = new Message();
						msg.what = SHOWTREATEDIMG;
						myHandler.sendMessage(msg);
						textResult = doOcr(bitmapTreated, LANGUAGE);
					}
					Message msg2 = new Message();
					msg2.what = SHOWRESULT;
					myHandler.sendMessage(msg2);
				}

			}).start();

		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	// 拍照识别
	class cameraButtonListener implements OnClickListener {

		@Override
		public void onClick(View arg0) {
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			intent.putExtra(MediaStore.EXTRA_OUTPUT,
					Uri.fromFile(new File(IMG_PATH, "temp.jpg")));
			startActivityForResult(intent, PHOTO_CAPTURE);
			dialog.cancel();
		}
	};

	// 从相册选取照片并裁剪
	class selectButtonListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			intent.setType("image/*");
			intent.putExtra("crop", "true");
			intent.putExtra("scale", true);
			intent.putExtra("return-data", false);
			intent.putExtra(MediaStore.EXTRA_OUTPUT,
					Uri.fromFile(new File(IMG_PATH, "temp_cropped.jpg")));
			intent.putExtra("outputFormat",
					Bitmap.CompressFormat.JPEG.toString());
			intent.putExtra("noFaceDetection", true); // no face detection
			startActivityForResult(intent, PHOTO_RESULT);
			dialog.cancel();
		}

	}

	
	// 将图片显示在view中
	public static void showPicture(ImageView iv, Bitmap bmp) {
		iv.setImageBitmap(bmp);
	}

	/**
	 * 进行图片识别
	 * 
	 * @param bitmap
	 *            待识别图片
	 * @param language
	 *            识别语言
	 * @return 识别结果字符串
	 */
	public String doOcr(Bitmap bitmap, String language) {
		TessBaseAPI baseApi = new TessBaseAPI();

		baseApi.init(getSDPath(), language);

		// 必须加此行，tess-two要求BMP必须为此配置
		bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

		baseApi.setImage(bitmap);

		String text = baseApi.getUTF8Text();

		baseApi.clear();
		baseApi.end();

		return text;
	}

	/**
	 * 获取sd卡的路径
	 * 
	 * @return 路径的字符串
	 */
	public static String getSDPath() {
		File sdDir = null;
		boolean sdCardExist = Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
		if (sdCardExist) {
			sdDir = Environment.getExternalStorageDirectory();// 获取外存目录
		}
		return sdDir.toString();
	}

	/**
	 * 调用系统图片编辑进行裁剪
	 */
	public void startPhotoCrop(Uri uri) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(uri, "image/*");
		intent.putExtra("crop", "true");
		intent.putExtra("scale", true);
		intent.putExtra(MediaStore.EXTRA_OUTPUT,
				Uri.fromFile(new File(IMG_PATH, "temp_cropped.jpg")));
		intent.putExtra("return-data", false);
		intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
		intent.putExtra("noFaceDetection", true); // no face detection
		startActivityForResult(intent, PHOTO_RESULT);
	}

	/**
	 * 根据URI获取位图
	 * 
	 * @param uri
	 * @return 对应的位图
	 */
	private Bitmap decodeUriAsBitmap(Uri uri) {
		Bitmap bitmap = null;
		try {
			bitmap = BitmapFactory.decodeStream(getContentResolver()
					.openInputStream(uri));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		return bitmap;
	}
}
