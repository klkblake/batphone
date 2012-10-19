package org.servalproject.auth;

import java.util.Random;

import org.servalproject.R;
import org.servalproject.ServalBatPhoneApplication;

import android.app.Activity;
import android.app.AlertDialog;
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

public class AuthSymbols extends Activity {

	private static final int REQUEST = 1;
	public static final String EXTRA_SYMBOL_GENERATOR_INDEX = "org.servalproject.auth.symbol_generator";

	private static final int MIN_ENTROPY = 64;
	private static final int MAX_ERRORS = 1;

	private static final int NUM_FAKE_SYMBOLS = 3;

	public enum State {
		YOU, THEM
	};

	private State state;

	private Random rand = new Random();

	private SymbolGenerator yours;
	private SymbolGenerator theirs;
	private SymbolGenerator dummy;

	private TextView group;
	private TextView entropyView;
	private TextView title;
	private FrameLayout symbol;
	private GridView possibleSymbols;
	private Button next;

	private View[] possibleSymbolViews;
	private int trueSymbol;

	private int total = 0;
	private boolean youSucceeded = false;
	private boolean theySucceeded = false;
	private int errors = 0;
	private double entropy = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.auth_symbol);

		int index = getIntent().getIntExtra(EXTRA_SYMBOL_GENERATOR_INDEX, -1);
		if (index == -1) {
			throw new IllegalArgumentException(EXTRA_SYMBOL_GENERATOR_INDEX
					+ " not set");
		}
		SymbolGeneratorFactory sgf = SymbolGenerators.get()[index];
		Random secrand = new Random(1234); // XXX session key goes here
		if (ServalBatPhoneApplication.context.callHandler.initiated) {
			state = State.THEM; // These are reversed, since next is called
								// before the first symbol.
			yours = sgf.getSymbolGenerator(new Random(secrand.nextLong()));
			theirs = sgf.getSymbolGenerator(new Random(secrand.nextLong()));
		} else {
			state = State.YOU;
			theirs = sgf.getSymbolGenerator(new Random(secrand.nextLong()));
			yours = sgf.getSymbolGenerator(new Random(secrand.nextLong()));
		}
		dummy = sgf.getSymbolGenerator(rand);

		group = (TextView) findViewById(R.id.auth_symbol_num);
		entropyView = (TextView) findViewById(R.id.auth_entropy);
		title = (TextView) findViewById(R.id.auth_symbol_title);
		symbol = (FrameLayout) findViewById(R.id.auth_symbol);
		possibleSymbols = (GridView) findViewById(R.id.auth_possible_symbols);
		possibleSymbolViews = new View[NUM_FAKE_SYMBOLS + 1];
		possibleSymbols.setAdapter(new ViewArrayAdapter(this,
				possibleSymbolViews, R.drawable.border));
		next = (Button) findViewById(R.id.auth_next_button);

		next();

		Button cancel = (Button) findViewById(R.id.auth_cancel_button);
		cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(AuthResult.CANCELLED);
				finish();
			}
		});

		next.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				correct();
			}
		});

		possibleSymbols.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (position == trueSymbol) {
					correct();
				} else {
					error();
				}
			}
		});
	}

	private void correct() {
		total++;
		switch (state) {
		case YOU:
			entropy += yours.getEntropy();
			youSucceeded = true;
			break;
		case THEM:
			entropy += theirs.getEntropy();
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
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Auth Succeeded");
		builder.show();
		setResult(AuthResult.SUCCESS);
		finish();
	}

	private void fail() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Auth Failed");
		builder.show();
		setResult(AuthResult.FAILURE);
		finish();
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
			symbol.addView(yours.getSymbolBlock(this));
			symbol.setVisibility(View.VISIBLE);
			possibleSymbols.setVisibility(View.GONE);
			next.setVisibility(View.VISIBLE);
			break;
		case THEM:
			title.setText(R.string.auth_their_symbol_title);
			symbol.setVisibility(View.GONE);
			trueSymbol = rand.nextInt(possibleSymbolViews.length);
			for (int i = 0; i < possibleSymbolViews.length; i++) {
				if (i == trueSymbol) {
					possibleSymbolViews[i] = theirs.getSymbolBlock(this);
				} else {
					possibleSymbolViews[i] = dummy.getSymbolBlock(this);
				}
			}
			((ViewArrayAdapter) possibleSymbols.getAdapter()).notifyChanged();
			possibleSymbols.setVisibility(View.VISIBLE);
			next.setVisibility(View.GONE);
			break;
		}
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
