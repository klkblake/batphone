package org.servalproject.auth;

import org.servalproject.R;
import org.servalproject.ServalBatPhoneApplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class AuthIntro extends Activity {

	private static final int REQUEST = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.auth_intro);

		final Spinner generator = (Spinner) findViewById(R.id.auth_generator_spinner);
		generator
				.setAdapter(new ArrayAdapter<SymbolGeneratorFactory>(this,
				android.R.layout.simple_spinner_item, SymbolGenerators
						.get()));

		Button cancel = (Button) findViewById(R.id.auth_cancel_button);
		cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(AuthResult.CANCELLED);
				finish();
			}
		});

		Button next = (Button) findViewById(R.id.auth_next_button);
		next.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				int index = generator.getSelectedItemPosition();
				if (index == AdapterView.INVALID_POSITION) {
					AlertDialog.Builder builder = new AlertDialog.Builder(
							AuthIntro.this);
					builder.setMessage(R.string.auth_no_generator);
					builder.show();
					return;
				}
				Intent intent = new Intent(
						ServalBatPhoneApplication.context,
						AuthSymbols.class);
				intent.putExtra(AuthSymbols.EXTRA_SYMBOL_GENERATOR_INDEX, index);
				startActivityForResult(intent, REQUEST);
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST && resultCode != AuthResult.BACK) {
			setResult(resultCode);
			finish();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		finishActivity(REQUEST);
	}

}
