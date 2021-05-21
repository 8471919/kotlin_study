package com.example.howlstagram_f16.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.howlstagram_f16.R
import com.example.howlstagram_f16.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_detail.view.*

class DetailViewFragment: Fragment() {
    var firestore : FirebaseFirestore? = null
    var uid : String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view = LayoutInflater.from(activity).inflate(R.layout.fragment_detail, container, false)
        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid

        view.detailviewfragment_recyclerview.adapter = DetailViewRecyclerViewAdapter()
        view.detailviewfragment_recyclerview.layoutManager = LinearLayoutManager(activity)
        return view
    }

    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTO : ArrayList<ContentDTO> = arrayListOf()
        var contentUidList : ArrayList<String> = arrayListOf()

        //데이터 받아옴
        init {
            firestore?.collection("images")?.orderBy("timestamp")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                contentDTO.clear()
                contentUidList.clear()
                //Sometimes, This code return null of querySnapshot when it signout
                if(querySnapshot == null) return@addSnapshotListener

                for(snapshot in querySnapshot!!.documents){
                    var item = snapshot.toObject(ContentDTO::class.java)
                    contentDTO.add(item!!)
                    contentUidList.add(snapshot.id)
                }
                //새로고침
                notifyDataSetChanged()
            }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail,parent,false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
            return contentDTO.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var viewholder = (holder as CustomViewHolder).itemView

            //유저 프로필 아이디
            viewholder.detailviewitem_profile_textview.text = contentDTO!![position].userId

            //유저 프로필 이미지
            Glide.with(holder.itemView.context).load(contentDTO!![position].imageUrl).into(viewholder.detailviewitem_profile_image)

            //업로드 이미지
            Glide.with(holder.itemView.context).load(contentDTO!![position].imageUrl).into(viewholder.detailviewitem_imageview_content)

            //업로드 내용
            viewholder.detailviewitem_explain_textview.text = contentDTO!![position].explain

            //좋아요 개수
            viewholder.detailviewitem_favoritecounter_textview.text="Likes "+ contentDTO!![position].favoriteCount

            viewholder.detailviewitem_favorite_imageview.setOnClickListener {
                favoriteEvent(position)
            }

            //좋아요 눌렀을 때 -> 꽉찬 하트, 안눌렀을 때 -> 빈 하트 설정
            if (contentDTO!![position].favorites.containsKey(uid)){
                viewholder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite)
            }else{
                viewholder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite_border)
            }

            //This code is when the profile image is clicked
            viewholder.detailviewitem_profile_image.setOnClickListener {
                var fragment = UserFragment()
                var bundle = Bundle()
                bundle.putString("destinationUid", contentDTO[position].uid)
                bundle.putString("userId", contentDTO[position].userId)
                fragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content,fragment)?.commit()

            }

        }

        fun favoriteEvent(position: Int){
            var tsDoc = firestore?.collection("images")?.document(contentUidList[position])
            firestore?.runTransaction { transaction ->
                var contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

                if(contentDTO!!.favorites.containsKey(uid)){
                    //좋아요 클릭되어 있을때


                    // 좋아요 개수 -1
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount - 1
                    // 유저 네임 제거
                    contentDTO?.favorites.remove(uid)

                }else{
                    //좋아요 클릭되지 않을때

                    //좋아요 개수 +1
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount + 1
                    //유저 네임 추가
                    contentDTO?.favorites[uid!!] = true

                }
                //서버로 돌려줌
                transaction.set(tsDoc,contentDTO)
            }


        }
    }
}