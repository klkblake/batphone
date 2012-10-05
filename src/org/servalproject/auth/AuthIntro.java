package org.servalproject.auth;

import org.servalproject.R;
import org.servalproject.ServalBatPhoneApplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class AuthIntro extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.auth_intro);

		final Spinner authGeneratorSpinner = (Spinner) findViewById(R.id.auth_generator_spinner);
		authGeneratorSpinner.setAdapter(new ArrayAdapter<SymbolGenerator>(this,
				android.R.layout.simple_spinner_item, SymbolGenerators
						.get()));

		Button authCancelButton = (Button) findViewById(R.id.auth_cancel_button);
		authCancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		Button authListsButton = (Button) findViewById(R.id.auth_lists_button);
		authListsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SymbolGenerator symGen = (SymbolGenerator) authGeneratorSpinner
						.getSelectedItem();
				if (symGen == null) {
					AlertDialog.Builder builder = new AlertDialog.Builder(
							AuthIntro.this);
					builder.setMessage(R.string.auth_no_generator);
					builder.show();
					return;
				}
				// XXX Testing
				TestSymbolGenerators.gen = symGen;
				startActivityForResult(new Intent(
						ServalBatPhoneApplication.context,
						TestSymbolGenerators.class), -1);
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_CANCELED) {
			setResult(resultCode);
			finish();
		}
	}

}
