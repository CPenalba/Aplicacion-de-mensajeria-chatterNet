package com.example.chatternet;

import static com.example.chatternet.utils.FirebaseUtil.currentUserId;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.chatternet.utils.FirebaseUtil;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.Map;

public class GroupFragment extends Fragment {

    private View groupFragmentView;
    private ListView list_view;
    private ArrayAdapter<String> arrayAdapter;
    private ArrayList<String> list_of_groups = new ArrayList<>();

    public GroupFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        groupFragmentView = inflater.inflate(R.layout.fragment_group, container, false);

        list_view = groupFragmentView.findViewById(R.id.list_view);

        list_view.setOnItemClickListener((parent, view, position, id) -> {
            String currentGroupName = list_of_groups.get(position);
            checkMembership(currentGroupName).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    boolean isMember = task.getResult();
                    if (isMember) {
                        Intent groupChatIntent = new Intent(getContext(), GroupChatActivity.class);
                        groupChatIntent.putExtra("groupName", currentGroupName);
                        getContext().startActivity(groupChatIntent);
                    } else {
                        showRequestAccessDialog(currentGroupName);
                    }
                } else {
                    Toast.makeText(getContext(), "Error al verificar la membresía", Toast.LENGTH_SHORT).show();
                }
            });
        });

        initializeFields();

        return groupFragmentView;
    }

    private void initializeFields() {
        list_view = groupFragmentView.findViewById(R.id.list_view);
        arrayAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, list_of_groups) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View itemView = super.getView(position, convertView, parent);
                TextView textView = itemView.findViewById(android.R.id.text1);

                String emoji = getRandomEmoji();

                textView.setText(emoji + " " + list_of_groups.get(position));

                return itemView;
            }
        };

        list_view.setAdapter(arrayAdapter);

        retrieveAndDisplayGroups();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bottom_navigation_menu, menu);
    }

    private String getRandomEmoji() {
        String[] emojis = {"\uD83D\uDE00", "\uD83D\uDE01", "\uD83D\uDE02", "\uD83D\uDE03", "\uD83D\uDE04", "\uD83D\uDE05"};
        int randomIndex = (int) (Math.random() * emojis.length);
        return emojis[randomIndex];
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.my_groups) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Task<Boolean> checkMembership(String groupName) {
        DocumentReference groupDocRef = FirebaseUtil.groupRef().document(groupName);
        TaskCompletionSource<Boolean> taskCompletionSource = new TaskCompletionSource<>();

        groupDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    Map<String, Boolean> members = (Map<String, Boolean>) document.get("members");
                    if (members != null && members.containsKey(currentUserId()) && members.get(currentUserId())) {
                        taskCompletionSource.setResult(true);
                    } else {
                        taskCompletionSource.setResult(false);
                    }
                } else {
                    taskCompletionSource.setResult(false);
                }
            } else {
                taskCompletionSource.setResult(false);
            }
        });

        return taskCompletionSource.getTask();
    }

    private void showRequestAccessDialog(String groupName) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.custom_dialog_layout, null);

        AlertDialog dialog = new AlertDialog.Builder(getContext()).setView(dialogView).create();

        TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
        TextView dialogMessage = dialogView.findViewById(R.id.dialog_message);
        Button positiveButton = dialogView.findViewById(R.id.positive_button);
        Button negativeButton = dialogView.findViewById(R.id.negative_button);

        dialogTitle.setText("Group Access");
        dialogMessage.setText("You are not a member of this group. Do you want to be part of it?");

        positiveButton.setOnClickListener(v -> {
            requestAccessToGroup(groupName);
            dialog.dismiss();
        });

        negativeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void approveAccessRequest(String groupName, String userId) {
        DocumentReference groupDocRef = FirebaseUtil.groupRef().document(groupName);

        groupDocRef.update("members." + userId, true, "accessRequests." + userId, FieldValue.delete()).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Access approved", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Error sending access request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void requestAccessToGroup(String groupName) {
        String userId = currentUserId();

        DocumentReference groupDocRef = FirebaseUtil.groupRef().document(groupName);

        groupDocRef.update("accessRequests." + userId, true).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Access request sent", Toast.LENGTH_SHORT).show();
            MainActivity mainActivity = MainActivity.getInstance();
            if (mainActivity != null) {
                mainActivity.createAccessRequest(groupName, userId, getContext());
            }

        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Error sending access request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void retrieveAndDisplayGroups() {
        FirebaseUtil.groupRef().addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    return;
                }

                if (value != null) {
                    list_of_groups.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        list_of_groups.add(doc.getId());
                    }
                    arrayAdapter.notifyDataSetChanged();
                }
            }
        });
    }
}
