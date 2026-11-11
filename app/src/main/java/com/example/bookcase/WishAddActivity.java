package com.example.bookcase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.*;
import androidx.lifecycle.*;
public class WishAddActivity extends BookcaseActivity {
    private AddModel model;private AlertDialog confirmation;
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);setContentView(R.layout.activity_wish_add);((TextView)findViewById(R.id.screenTitle)).setText(R.string.add_wish);
        findViewById(R.id.backButton).setOnClickListener(v->finish());model=new ViewModelProvider(this).get(AddModel.class);
        findViewById(R.id.saveWish).setOnClickListener(v->{
            try {model.prepare(WishItem.create(((EditText)findViewById(R.id.wishTitleInput)).getText().toString(),
                    ((EditText)findViewById(R.id.wishAuthorInput)).getText().toString(),((EditText)findViewById(R.id.wishIsbnInput)).getText().toString()));}
            catch (IllegalArgumentException invalid){((TextView)findViewById(R.id.wishSaveStatus)).setText(invalid.getMessage());}
        });
        model.phase.observe(this,phase->{
            if(phase==AddModel.DONE){finish();return;}
            boolean enabled=phase==AddModel.READY || phase==AddModel.ERROR;
            for(int id:new int[]{R.id.saveWish,R.id.wishTitleInput,R.id.wishAuthorInput,R.id.wishIsbnInput})findViewById(id).setEnabled(enabled);
            ((TextView)findViewById(R.id.wishSaveStatus)).setText(phase==AddModel.ERROR?getString(model.error)
                    :phase==AddModel.CHECKING?getString(R.string.wish_checking):phase==AddModel.SAVING?getString(R.string.wish_saving):"");
            if(phase==AddModel.CONFIRM && confirmation==null){
                confirmation=new AlertDialog.Builder(this).setMessage(model.match==WishlistMatches.Match.ISBN?R.string.wish_confirm_owned:R.string.wish_confirm_similar)
                        .setPositiveButton(R.string.wish_add_anyway,(d,w)->model.commit())
                        .setNegativeButton(R.string.cancel_action,(d,w)->model.phase.setValue(AddModel.READY)).create();
                confirmation.setOnCancelListener(d->model.phase.setValue(AddModel.READY));
                confirmation.setOnDismissListener(d->confirmation=null);confirmation.show();
            }
        });
    }
    @Override protected void onDestroy(){if(confirmation!=null)confirmation.dismiss();super.onDestroy();}
    public static class AddModel extends ViewModel {
        static final int READY=0,CHECKING=1,CONFIRM=2,SAVING=3,DONE=4,ERROR=5;
        final MutableLiveData<Integer> phase=new MutableLiveData<>(READY);
        private WishItem pending;WishlistMatches.Match match;int error;
        void prepare(WishItem wish){
            if(phase.getValue()!=READY && phase.getValue()!=ERROR)return;
            pending=wish;phase.setValue(CHECKING);
            new BookRepository().getBooks(null).addOnSuccessListener(books->{match=WishlistMatches.owned(wish,books);
                if(match==WishlistMatches.Match.NONE)commit();else phase.setValue(CONFIRM);
            }).addOnFailureListener(e->{error=R.string.wishlist_load_failed;phase.setValue(ERROR);});
        }
        void commit(){
            if(pending==null || phase.getValue()==SAVING)return;phase.setValue(SAVING);
            new WishlistRepository().add(pending).addOnSuccessListener(ignored->phase.setValue(DONE))
                    .addOnFailureListener(e->{error=R.string.wish_save_failed;
                        for(Throwable cause=e;cause!=null;cause=cause.getCause())if(cause.getMessage()!=null && cause.getMessage().contains("already on your wishlist"))error=R.string.wish_duplicate;
                        phase.setValue(ERROR);});
        }
    }
}
