#include <jni.h>
#include <opencv2/opencv.hpp>
#include <vector>
#include <android/log.h>
#include <sstream>

#define APPNAME "BookAlive"
#define PATH "/mnt/sdcard/Pictures/BookAlive/"
using namespace cv;
using namespace std;

extern "C" {

Mat findHomography(Mat orig, Mat test) {
    vector<KeyPoint> kp_orig, kp_test;
    FAST(orig, kp_orig, 80);
    FAST(test, kp_test, 80);
    FREAK ext;
    Mat desc_orig, desc_test;
    ext.compute(orig, kp_orig, desc_orig);
    ext.compute(test, kp_test, desc_test);

    BFMatcher matcher(NORM_HAMMING);
    vector<DMatch> matches;
    matcher.match(desc_orig, desc_test, matches);

    double min_dist = 100, max_dist = 0;
    for(size_t i=0; i<desc_orig.rows; i++) {
        double dist = matches[i].distance;
        if(dist < min_dist) min_dist = dist;
        if(dist > max_dist) max_dist = dist;
    }
    double acceptable_dist = 3*min_dist;
    vector<DMatch> good_matches;
    for(size_t i=0; i<desc_orig.rows; i++) {
        if(matches[i].distance < acceptable_dist) {
            good_matches.push_back(matches[i]);
        }
    }
    vector<Point2f> orig_pts;
    vector<Point2f> test_pts;

    for( size_t i = 0; i < good_matches.size(); i++ ) {
        //-- Get the keypoints from the good matches
        orig_pts.push_back( kp_orig[ good_matches[i].queryIdx ].pt );
        test_pts.push_back( kp_test[ good_matches[i].trainIdx ].pt );
    }
    Mat H = findHomography( orig_pts, test_pts, CV_RANSAC );
    return H;
}

Mat getHomography(Mat orig, Mat test) {
    vector<KeyPoint> kp_orig, kp_test;
    FAST(orig, kp_orig, 10);
    FAST(test, kp_test, 10);
    __android_log_write(ANDROID_LOG_INFO, "vision.cpp", "Keypoints computed");

    // TODO remove this
    char temp[50];
    sprintf(temp, "%d, %d", kp_test.size(), kp_orig.size());
    __android_log_write(ANDROID_LOG_INFO, "vision.cpp-size", temp);

    FREAK ext;
    Mat desc_orig, desc_test;
    ext.compute(orig, kp_orig, desc_orig);
    ext.compute(test, kp_test, desc_test);

    BFMatcher matcher(NORM_HAMMING);
    vector<DMatch> matches;
    matcher.match(desc_orig, desc_test, matches);

    __android_log_write(ANDROID_LOG_INFO, "vision.cpp", "Matching done");

    double min_dist = 100, max_dist = 0;
    for(int i=0; i<desc_orig.rows; i++) {
    	DMatch d = matches[i];
        double dist = d.distance;
        if(dist < min_dist) min_dist = dist;
        if(dist > max_dist) max_dist = dist;
    }
    double acceptable_dist = 3*min_dist;
    vector<DMatch> good_matches;
    for(int i=0; i<desc_orig.rows; i++) {
    	DMatch d = matches[i];
        if(d.distance < acceptable_dist) {
            good_matches.push_back(d);
        }
    }
    vector<Point2f> orig_pts;
    vector<Point2f> test_pts;

    for( int i = 0; i < good_matches.size(); i++ ) {
        //-- Get the keypoints from the good matches
    	DMatch match = good_matches[i];
    	KeyPoint kp1 = kp_orig[ match.queryIdx ];
    	KeyPoint kp2 = kp_orig[ match.trainIdx ];
        orig_pts.push_back( kp1.pt );
        test_pts.push_back( kp2.pt );
    }
    Mat H = findHomography( orig_pts, test_pts, CV_RANSAC );
    __android_log_write(ANDROID_LOG_INFO, "vision.cpp", "Computed Homography");

    return H;
}

void setSize(Mat orig1, Mat test1, Mat& orig, Mat& test) {
	double facX = 1, facY = 1;
	resize(orig1, orig, Size(0,0), facX, facY);
	resize(test1, test, Size(0,0), facX, facY);
	cvtColor(orig, orig, CV_BGR2GRAY);
	cvtColor(test, test, CV_BGR2GRAY);
}

JNIEXPORT void JNICALL Java_com_example_paintingrestore_CameraActivity_computeOverlayPosition(JNIEnv*, jobject) {
	__android_log_write(ANDROID_LOG_INFO, "vision.cpp", "function called in cpp");
}

JNIEXPORT void JNICALL Java_com_rohit_bookalive_CapturedImage_computeHomography(JNIEnv*, jobject, jlong addrOrig, jlong addrImg, jlong addrH) {
	Mat& Orig1 = *(Mat*)addrOrig;
	Mat& Img1 = *(Mat*)addrImg;

	Mat Orig, Img;
	setSize(Orig1, Img1, Orig, Img);

	Mat& H = *(Mat*)addrH;
	H = findHomography(Orig, Img);
	char res[50];
	sprintf(res, "%lf %lf %lf", H.at<double>(0,0), H.at<double>(0,1), H.at<double>(0,2));
	__android_log_write(ANDROID_LOG_INFO, "vision.cpp-H", res);
}

JNIEXPORT void JNICALL Java_com_rohit_bookalive_Util_mapPoint(JNIEnv*, jobject, jlong addrH, jlong addrP) {
	Mat& Pnt = *(Mat*)addrP;
	Mat& H = *(Mat*)addrH;
	Point2f p, p2;
	p.x =Pnt.at<double>(0,0); p.y =Pnt.at<double>(0,1);
	vector<Point2f> pt, pt2; pt.push_back(p);
	perspectiveTransform(pt, pt2, H);
	p2 = pt2[0];
	Pnt.at<double>(0,0) = p2.x; Pnt.at<double>(0,1) = p2.y;
}

double getDist2(Mat a, Mat b) {
    Mat a1, b1;
    a.convertTo(a1, CV_32FC1);
    b.convertTo(b1, CV_32FC1);
    Mat temp = a1-b1;
    return norm(temp);
}

vector<int> computeVisualWords(Mat desc, Mat vocab) {
    vector<int> corr_vocab_word;
    for(size_t i=0; i<desc.rows; i++) {
        int minD = 99999999;
        int minI = 0;
        for(size_t j=0; j<vocab.rows; j++) {
            double d = getDist2(desc.row(i), vocab.row(j));
            if(d < minD) {
                minD = d;
                minI = j;
            }
        }
        corr_vocab_word.push_back(minI);
    }
    return corr_vocab_word;
}

vector<int> computeHist(vector<int> visual_words, int nbins) {
    vector<int> hist;
    for(int i=0; i<nbins; i++) hist.push_back(0);
    for(size_t i=0; i<visual_words.size(); i++) {
        hist[visual_words[i]] ++;
    }
    return hist;
}

JNIEXPORT jint JNICALL Java_com_rohit_bookalive_ImageMatcher_getPageNum(JNIEnv*, jobject, jlong addrTest) {
    Mat hists, keys, hists_d;
    string path = string(PATH) + "Pages/hists.yml";
    string vocab_path = string(PATH) + "Pages/vocab.yml";
    FileStorage fs = FileStorage(path.c_str(), FileStorage::READ);
    fs["hists"] >> hists;
    fs["keys"] >> keys;
    fs.release();
    hists.convertTo(hists_d, CV_32FC1);

    Mat vocab;
    fs = FileStorage(vocab_path.c_str(), FileStorage::READ);
    fs["vocabulary"] >> vocab;
    fs.release();
    
    Mat& test = *(Mat*)addrTest;
    // Using a ORB detector with less keypoints to work faster
    ORB de = ORB(100);          // Train using 100 keypoints (Offline/train.cpp)

    vector<KeyPoint> kp;
    Mat desc;
    de(test, Mat(), kp, desc);
    vector<int> visual_words = computeVisualWords(desc, vocab);
    vector<int> hist = computeHist(visual_words, vocab.rows);
    Mat temp = Mat(hist);
    Mat temp_d;
    temp.convertTo(temp_d, CV_32FC1);

    
    
    // TODO remove this
    char temps[50];
    Mat p = hists_d.row(0).t();
    sprintf(temps, "%d, %d", temp_d.rows, p.rows);
    __android_log_write(ANDROID_LOG_INFO, "vision.cpp-size", temps);


    
    int mx_dot = 0, mx_i;
    for(size_t i=0; i<hists_d.rows; i++) {
        double dot = hists_d.row(i).t().dot(temp_d);
        if(mx_dot < dot) {
            mx_dot = dot;
            mx_i = i;
        }
    }
    jint res = keys.at<int>(mx_i);
    return res;
}

}
