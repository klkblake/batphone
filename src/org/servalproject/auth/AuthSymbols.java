package org.servalproject.auth;

import java.util.Random;

import org.servalproject.R;
import org.servalproject.ServalBatPhoneApplication;
import org.servalproject.servald.AbstractIdRandom;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

public class AuthSymbols extends Activity {

	private static final int REQUEST = 1;
	public static final String EXTRA_SYMBOL_GENERATOR_INDEX = "org.servalproject.auth.symbol_generator";

	private static final int MIN_ENTROPY = 320;
	private static final int MAX_ERRORS = 1;

	private static final int NUM_FAKE_SYMBOLS = 3;

	public enum State {
		YOU, THEM
	};

	private State state;

	private Random rand = new Random();

	private SymbolGenerator symgen;
	private SymbolGenerator dummy;

	private TextView group;
	private TextView entropyView;
	private TextView title;
	private FrameLayout symbol;
	private GridView possibleSymbolsGrid;
	private Button noMatch;
	private TextView query;
	private Button yes, no;

	private Symbol[] possibleSymbols;
	private int trueSymbol;

	private int total = 0;
	private boolean youSucceeded = false;
	private boolean theySucceeded = false;
	private int errors = 0;
	private double entropy = 0;
	private double entropyDelta;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.auth_symbol);

		int index = getIntent().getIntExtra(EXTRA_SYMBOL_GENERATOR_INDEX, -1);
		if (index == -1) {
			throw new IllegalArgumentException(EXTRA_SYMBOL_GENERATOR_INDEX
					+ " not set");
		}
		SymbolGeneratorFactory factory = SymbolGenerators.get()[index];
		entropyDelta = factory.entropy;
		symgen = factory.create(new AbstractIdRandom(
				ServalBatPhoneApplication.context.callHandler.getAuthToken()));
		state = ServalBatPhoneApplication.context.callHandler.initiated ? State.THEM
				: State.YOU; // These are reversed, since next is called before
								// the first symbol.
		dummy = factory.create(rand);

		group = (TextView) findViewById(R.id.auth_symbol_num);
		entropyView = (TextView) findViewById(R.id.auth_entropy);
		title = (TextView) findViewById(R.id.auth_symbol_title);
		symbol = (FrameLayout) findViewById(R.id.auth_symbol);
		possibleSymbolsGrid = (GridView) findViewById(R.id.auth_possible_symbols);
		possibleSymbols = new Symbol[NUM_FAKE_SYMBOLS + 1];
		possibleSymbolsGrid.setAdapter(new SymbolArrayAdapter(this,
				possibleSymbols));
		noMatch = (Button) findViewById(R.id.auth_no_match);
		query = (TextView) findViewById(R.id.auth_query);
		yes = (Button) findViewById(R.id.auth_yes_button);
		no = (Button) findViewById(R.id.auth_no_button);

		next();

		Button cancel = (Button) findViewById(R.id.auth_cancel_button);
		cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(AuthResult.CANCELLED);
				finish();
			}
		});

		noMatch.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				error();
			}
		});

		yes.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				correct();
			}
		});

		no.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				error();
			}
		});

		possibleSymbolsGrid.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (position == trueSymbol) {
					showToast("Correct");
					correct();
				} else {
					showToast("Incorrect");
					error();
				}
			}
		});
	}

	private void showToast(String msg) {
		Toast.makeText(ServalBatPhoneApplication.context, msg,
				Toast.LENGTH_SHORT).show();
	}

	private void correct() {
		total++;
		entropy += entropyDelta;
		switch (state) {
		case YOU:
			youSucceeded = true;
			break;
		case THEM:
			theySucceeded = true;
			break;
		}
		eval();
	}

	private void error() {
		total++;
		errors++;
		entropy = 0;
		eval();
	}

	// XXX This does not currently take into account how much entropy is in each
	// symbol.
	private void eval() {
		if (youSucceeded && theySucceeded && entropy >= MIN_ENTROPY) {
			succeed();
		} else if (errors > MAX_ERRORS) {
			fail();
		} else {
			next();
		}
	}

	private void succeed() {
		startFinished(true);
	}

	private void fail() {
		startFinished(false);
	}

	private void startFinished(boolean success) {
		Intent intent = new Intent(this, AuthFinished.class);
		intent.putExtra(AuthFinished.EXTRA_SUCCEEDED, success);
		intent.putExtra(AuthFinished.EXTRA_IN_CONTACTS, getIntent()
				.getBooleanExtra(AuthFinished.EXTRA_IN_CONTACTS, false));
		startActivityForResult(intent, REQUEST);
	}

	private void next() {
		group.setText(getString(R.string.auth_symbol_num, total));
		entropyView.setText(getString(R.string.auth_entropy,
				entropy));

		switch (state) {
		case YOU:
			state = State.THEM;
			break;
		case THEM:
			state = State.YOU;
			break;
		}

		switch (state) {
		case YOU:
			title.setText(R.string.auth_your_symbol_title);
			symbol.removeAllViews();
			symbol.addView(symgen.next().getView(this, null));
			symbol.setVisibility(View.VISIBLE);
			possibleSymbolsGrid.setVisibility(View.GONE);
			noMatch.setVisibility(View.GONE);
			query.setVisibility(View.VISIBLE);
			yes.setVisibility(View.VISIBLE);
			no.setVisibility(View.VISIBLE);
			break;
		case THEM:
			title.setText(R.string.auth_their_symbol_title);
			symbol.setVisibility(View.GONE);
			trueSymbol = rand.nextInt(possibleSymbols.length);
			for (int i = 0; i < possibleSymbols.length; i++) {
				if (i == trueSymbol) {
					possibleSymbols[i] = symgen.next();
				} else {
					possibleSymbols[i] = dummy.next();
				}
			}
			((SymbolArrayAdapter) possibleSymbolsGrid.getAdapter())
					.notifyChanged();
			possibleSymbolsGrid.setVisibility(View.VISIBLE);
			noMatch.setVisibility(View.VISIBLE);
			query.setVisibility(View.GONE);
			yes.setVisibility(View.GONE);
			no.setVisibility(View.GONE);
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST && resultCode != AuthResult.BACK) {
			setResult(resultCode, data);
			finish();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		finishActivity(REQUEST);
	}

}