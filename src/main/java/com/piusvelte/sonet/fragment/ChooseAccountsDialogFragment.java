package com.piusvelte.sonet.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;

import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.provider.Accounts;

import java.util.List;
import java.util.Map;

import static com.piusvelte.sonet.Sonet.INVALID_ACCOUNT_ID;

/**
 * Created by bemmanuel on 4/14/15.
 */
public class ChooseAccountsDialogFragment {

    private static final String ARG_IDS = "ids";
    private static final String ARG_SERVICES = "services";

    public static MultiChoiceDialogFragment newInstance(@NonNull List<Accounts.Account> accounts, @NonNull Map<Long, Integer> selectedAccounts, @NonNull String title, int requestCode) {
        int i = 0;
        CharSequence[] items = new CharSequence[accounts.size()];
        boolean[] which = new boolean[items.length];
        long[] ids = new long[items.length];
        int[] services = new int[items.length];

        for (Accounts.Account account : accounts) {
            items[i] = account.username;
            which[i] = selectedAccounts.containsKey(account.id);
            ids[i] = account.id;
            services[i] = account.service;
            i++;
        }

        MultiChoiceDialogFragment dialogFragment = MultiChoiceDialogFragment.newInstance(items, which, title, requestCode);
        Bundle args = dialogFragment.getArguments();
        args.putLongArray(ARG_IDS, ids);
        args.putIntArray(ARG_SERVICES, services);
        return dialogFragment;
    }

    public static long getAccountId(MultiChoiceDialogFragment multiChoiceDialogFragment, int which) {
        Bundle args = multiChoiceDialogFragment.getArguments();
        long[] ids = args.getLongArray(ARG_IDS);

        if (which >= 0 && ids != null && ids.length > which) {
            return ids[which];
        }

        return INVALID_ACCOUNT_ID;
    }

    public static int getService(MultiChoiceDialogFragment multiChoiceDialogFragment, int which) {
        Bundle args = multiChoiceDialogFragment.getArguments();
        int[] services = args.getIntArray(ARG_SERVICES);

        if (which >= 0 && services != null && services.length > which) {
            return services[which];
        }

        return -1;
    }
}
